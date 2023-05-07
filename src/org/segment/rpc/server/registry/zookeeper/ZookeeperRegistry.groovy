package org.segment.rpc.server.registry.zookeeper

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.zkclient.ZkClient
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.common.RpcConf
import org.segment.rpc.common.ZkClientHolder
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.registry.*

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
@Slf4j
class ZookeeperRegistry implements Registry {
    private RpcConf c

    private ScheduledExecutorService scheduler

    private List<RemoteUrl> remoteUrlListCachedLocal = new ArrayList<RemoteUrl>()

    private Map<String, List<RemoteUrl>> remoteUrlListCachedLocalByContext = new HashMap<String, List<RemoteUrl>>()

    private EventHandler eventHandler = new EventHandler()

    List<RemoteUrl> getRemoveUrlListCachedLocal() {
        return remoteUrlListCachedLocal
    }

    private AtomicInteger count = new AtomicInteger(0)

    void refreshToLocal(boolean initReadyFalse = true) {
        def zkClient = connect()
        def pathPrefix = c.getString('zookeeper.path.prefix', '/segment-rpc')

        // not created yet
        if (!zkClient.exists(pathPrefix)) {
            return
        }

        def contextList = zkClient.getChildren(pathPrefix)
        if (!contextList) {
            return
        }

        List<RemoteUrl> getList = []
        for (path in contextList) {
            def addressList = zkClient.getChildren(pathPrefix + '/' + path)
            if (!addressList) {
                continue
            }

            // path -> demo
            // context -> /demo
            // TIPS: one server only support one context
            String context = '/' + path
            // address -> host:port
            def mapper = new ObjectMapper()
            for (address in addressList) {
                def arr = address.split(':')
                String host = arr[0]
                int port = arr[1] as int
                def data = zkClient.readData(pathPrefix + context + '/' + address)
                def remoteUrl = mapper.readValue(data, RemoteUrl)
                // overwrite optional
                remoteUrl.host = host
                remoteUrl.port = port
                getList << remoteUrl
            }
        }

        def loopCount = count.getAndIncrement()
        if (loopCount % 100 == 0) {
            log.info 'get list from registry: {}, loop count {}', getList.collect { it.toStringView() }.toString(), loopCount
            log.info 'local cached list: {}', remoteUrlListCachedLocal.collect { it.toStringView() }.toString()
        }

        boolean isLocalNeedUpdate = false

        // do merge list to local
        for (one in getList) {
            def localOne = remoteUrlListCachedLocal.find { it == one }
            if (localOne) {
                if (localOne.toStringView() == one.toStringView()) {
                    continue
                }

                // if local ready is false, need try connect
                boolean isNeedFire = !localOne.ready

                localOne.ready = one.ready
                localOne.weight = one.weight
                localOne.updatedTime = one.updatedTime
                localOne.extend(one.params)

                if (isNeedFire) {
                    eventHandler.fire(one, EventType.NEW_ADDED)
                }

                isLocalNeedUpdate = true
            } else {
                // set ready false and trigger client do connect first and then set ready true when channel is active
                if (initReadyFalse) {
                    one.ready = false
                }
                remoteUrlListCachedLocal << one
                log.info 'added new one - ' + one.toStringView()
                eventHandler.fire(one, EventType.NEW_ADDED)

                isLocalNeedUpdate = true
            }
        }

        def it = remoteUrlListCachedLocal.iterator()
        while (it.hasNext()) {
            def one = it.next()
            if (!getList.contains(one)) {
                it.remove()
                log.info 'removed old one - ' + one.toStringView()
                eventHandler.fire(one, EventType.OLD_REMOVED)

                isLocalNeedUpdate = true
            }
        }

        if (isLocalNeedUpdate) {
            log.info 'refresh local list'
            remoteUrlListCachedLocal.each {
                log.info it.toStringView()
            }
            remoteUrlListCachedLocal.groupBy { it.context }.each { context, subList ->
                remoteUrlListCachedLocalByContext.put(context, subList.sort())
            }
        }
    }

    private ZkClient connect() {
        def connectString = c.getString('zookeeper.connect.string', '127.0.0.1:2181')
        ZkClientHolder.instance.connect(connectString,
                c.getInt('zookeeper.connect.sessionTimeout', 30 * 1000),
                c.getInt('zookeeper.connect.connectionTimeout', 10 * 1000))
    }

    @Override
    void init(RpcConf c) {
        this.c = c
        // not rpc client, need not get registry servers' address
        if (!c.isOn('is.client.running')) {
            return
        }

        initEventHandler()
        refreshToLocal()

        // use interval, simple
        scheduler = Executors.newSingleThreadScheduledExecutor()

        final int interval = c.getInt('client.refresh.registry.interval.seconds', 2)

        def now = new Date()
        int sec = now.seconds
        long delaySeconds = interval - (sec % interval)

        scheduler.scheduleWithFixedDelay({
            try {
                refreshToLocal()
            } catch (Exception e) {
                log.error('refresh-registry-to-local error', e)
            }
        }, delaySeconds, interval, TimeUnit.SECONDS)
    }

    private void initEventHandler() {
        eventHandler.add(new EventTrigger() {
            @Override
            EventType type() {
                EventType.ACTIVE
            }

            @Override
            void handle(RemoteUrl remoteUrl) {
                for (one in getRemoveUrlListCachedLocal()) {
                    if (one == remoteUrl) {
                        one.ready = true
                        log.info 'channel active, remove server: {}, set ready = true', one
                    }
                }
            }
        })

        eventHandler.add(new EventTrigger() {
            @Override
            EventType type() {
                EventType.INACTIVE
            }

            @Override
            void handle(RemoteUrl remoteUrl) {
                ZookeeperRegistry.this.unavailable(remoteUrl)
            }
        })
    }

    @Override
    void register(RemoteUrl remoteUrl) {
        def pathPrefix = c.getString('zookeeper.path.prefix', '/segment-rpc')
        final String path = pathPrefix + remoteUrl.context

        def zkClient = connect()
        if (!zkClient.exists(path)) {
            zkClient.createPersistent(path, true)
            log.info 'ready to registry, created zookeeper path: {}', path
        }

        def mapper = new ObjectMapper()
        def data = mapper.writeValueAsBytes(remoteUrl)

        def targetPath = path + '/' + remoteUrl.toString()
        log.info 'ready to registry, target zookeeper path: {}', targetPath

        if (zkClient.exists(targetPath)) {
            // overwrite, for dashboard management
            zkClient.writeData(targetPath, data)
        } else {
            zkClient.createEphemeral(targetPath, data)
        }
        log.info 'done write registry data: {}', remoteUrl.toStringView()
    }

    @Override
    void unavailable(RemoteUrl remoteUrl) {
        synchronized (remoteUrlListCachedLocal) {
            def target = remoteUrlListCachedLocal.find { it == remoteUrl }
            if (target) {
                target.ready = false
                log.info 'channel inactive, remote server: {}, set ready = false', target
            }
        }
    }

    @Override
    List<RemoteUrl> discover(Req req) {
        remoteUrlListCachedLocalByContext.get(req.context())?.findAll { it.ready && it.weight > 0 }
    }

    @Override
    void addEvent(EventTrigger trigger) {
        eventHandler.add(trigger)
    }

    @Override
    void fire(RemoteUrl remoteUrl, EventType type) {
        log.info 'event fire, remote server: {}, type: {}', remoteUrl, type
        eventHandler.fire(remoteUrl, type)
    }

    @Override
    void shutdown() {
        if (scheduler) {
            scheduler.shutdown()
            log.info 'refresh-registry-to-local shutdown'
        }

        def connectString = c.getString('zookeeper.connect.string', '127.0.0.1:2181')
        ZkClientHolder.instance.disconnect(connectString)
    }
}
