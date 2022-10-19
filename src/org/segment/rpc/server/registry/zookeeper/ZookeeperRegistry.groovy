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

    private List<RemoteUrl> cachedLocalList = new LinkedList<RemoteUrl>()

    List<RemoteUrl> getCachedLocalList() {
        return cachedLocalList
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

        count.getAndIncrement()
        def number = count.get()
        if (number % 100 == 0) {
            log.info 'get list from registry {} count {}', getList.collect { it.toStringView() }.toString(), number
            log.info 'local list {}', cachedLocalList.collect { it.toStringView() }.toString()
        }

        // do merge list to local
        for (one in getList) {
            def localOne = cachedLocalList.find { it == one }
            if (localOne) {
                localOne.ready = one.ready
                localOne.weight = one.weight
                localOne.updatedTime = one.updatedTime
                localOne.extend(one.params)
            } else {
                // set ready false and trigger client do connect first and then set ready true when channel is active
                if (initReadyFalse) {
                    one.ready = false
                }
                cachedLocalList << one
                log.info 'added new one - ' + one.toStringView()
                EventHandler.instance.fire(one, EventType.NEW_ADDED)
            }
        }

        def it = cachedLocalList.iterator()
        while (it.hasNext()) {
            def one = it.next()
            if (!getList.contains(one)) {
                it.remove()
                log.info 'removed old one - ' + one.toStringView()
                EventHandler.instance.fire(one, EventType.OLD_REMOVED)
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

        final int interval = c.getInt('client.refresh.registry.interval.seconds', 10)

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
        EventHandler.instance.add(new EventTrigger() {
            @Override
            EventType type() {
                EventType.ACTIVE
            }

            @Override
            void handle(RemoteUrl remoteUrl) {
                for (one in cachedLocalList) {
                    if (one == remoteUrl) {
                        one.ready = true
                        log.info 'channel is active {} set ready = true', one
                    }
                }
            }
        })

        EventHandler.instance.add(new EventTrigger() {
            @Override
            EventType type() {
                EventType.INACTIVE
            }

            @Override
            void handle(RemoteUrl remoteUrl) {
                for (one in cachedLocalList) {
                    if (one == remoteUrl) {
                        one.ready = false
                        log.info 'channel is inactive {} set ready = false', one
                    }
                }
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
            log.info 'created path {}', path
        }

        def mapper = new ObjectMapper()
        def data = mapper.writeValueAsBytes(remoteUrl)

        def targetPath = path + '/' + remoteUrl.toString()
        log.info 'ready to registry to zookeeper path {}', targetPath

        if (zkClient.exists(targetPath)) {
            // overwrite, for dashboard management
            zkClient.writeData(targetPath, data)
        } else {
            zkClient.createEphemeral(targetPath, data)
        }
        log.info 'done write data {}', remoteUrl.toStringView()
    }

    @Override
    List<RemoteUrl> discover(Req req) {
        def context = req.context()
        if (context == null) {
            return []
        }
        cachedLocalList.findAll { context == it.context && it.ready }
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
