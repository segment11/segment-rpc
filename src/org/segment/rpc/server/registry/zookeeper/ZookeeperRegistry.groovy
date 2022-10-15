package org.segment.rpc.server.registry.zookeeper

import com.github.zkclient.ZkClient
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.common.Conf
import org.segment.rpc.common.ZkClientHolder
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.registry.Registry
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
@Singleton
@Slf4j
class ZookeeperRegistry implements Registry {
    private final static String DATE_FORMAT = 'yyyy-MM-dd HH:mm:ss'

    private ScheduledExecutorService scheduler

    private Conf c = Conf.instance

    private List<RemoteUrl> cachedLocalList = new CopyOnWriteArrayList<RemoteUrl>()

    private AtomicInteger count = new AtomicInteger(0)

    private void refreshToLocal() {
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
            for (address in addressList) {
                def arr = address.split(':')
                String host = arr[0]
                int port = arr[1] as int

                def map = readData(zkClient.readData(pathPrefix + context + '/' + address))
                def remoteUrl = new RemoteUrl(host, port)
                remoteUrl.context = context
                remoteUrl.metricExportPort = map['metricExportPort'] as int
                remoteUrl.ready = Boolean.valueOf(map['ready'])
                remoteUrl.weight = map['weight'] as int
                remoteUrl.updatedTime = Date.parse(DATE_FORMAT, map['updateTime'])
                getList << remoteUrl
            }
        }

        count.getAndIncrement()
        def number = count.get()
        if (number % 10 == 0) {
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
            } else {
                cachedLocalList << one
            }
        }
        cachedLocalList.removeAll {
            !(it in getList)
        }
    }

    private HashMap<String, String> readData(byte[] data) {
        HashMap<String, String> r = [:]
        // str -> ready=true,updateDate=**
        def str = new String(data)
        str.split(',').each { String one ->
            def arr = one.split('=')
            r[arr[0]] = arr[1]
        }
        r
    }

    private ZkClient connect() {
        def connectString = c.getString('zookeeper.connect.string', '127.0.0.1:2181')
        ZkClientHolder.instance.connect(connectString,
                c.getInt('zookeeper.connect.sessionTimeout', 30 * 1000),
                c.getInt('zookeeper.connect.connectionTimeout', 10 * 1000))
    }

    @Override
    void init() {
        // not rpc client, need not get registry servers' address
        if (!c.isOn('is.client.running')) {
            return
        }
        refreshToLocal()

        // use interval, simple
        scheduler = Executors.newSingleThreadScheduledExecutor()

        final int interval = Conf.instance.getInt('client.refresh.registry.interval.seconds', 10)

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

    @Override
    void register(RemoteUrl url) {
        def pathPrefix = c.getString('zookeeper.path.prefix', '/segment-rpc')
        log.info 'ready to registry {} to zookeeper with path {}{}',
                url.toString(), pathPrefix, url.context
        final String path = pathPrefix + url.context

        def zkClient = connect()
        if (!zkClient.exists(path)) {
            zkClient.createPersistent(path, true)
            log.info 'created path {}', path
        }

        def data = "ready=${url.ready},weight=${url.weight},metricExportPort=${url.metricExportPort}," +
                "updateTime=${url.updatedTime.format(DATE_FORMAT)}"
        def targetPath = path + '/' + url.toString()
        if (zkClient.exists(targetPath)) {
            zkClient.writeData(targetPath, data.toString().bytes)
        } else {
            zkClient.createEphemeral(targetPath, data.toString().bytes)
        }
        log.info 'done write data {}', url.toStringView()
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
