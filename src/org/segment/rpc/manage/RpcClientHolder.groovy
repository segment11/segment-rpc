package org.segment.rpc.manage


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.client.RpcClient
import org.segment.rpc.common.RpcConf
import org.segment.rpc.server.registry.zookeeper.ZookeeperRegistry

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Singleton
@Slf4j
// for dashboard
class RpcClientHolder {
    private Map<String, RpcClient> cached = new ConcurrentHashMap<>()
    private Map<String, ZookeeperRegistry> registryCached = new ConcurrentHashMap<>()

    synchronized ZookeeperRegistry getRegistry(Map map) {
        String key = map.toString()
        def old = registryCached[key]
        if (old) {
            return old
        }

        def c = new RpcConf().extend(map) as RpcConf
        def one = new ZookeeperRegistry()
        one.init(c)
        registryCached[key] = one
        one
    }

    synchronized RpcClientWapper create(String connectString, String prefix) {
        final String key = connectString + prefix
        def client = cached[key]
        if (client) {
            return new RpcClientWapper(client: client)
        }

        Map<String, Object> map = [:]
        map[RpcConf.ZK_CONNECT_STRING] = connectString
        map[RpcConf.ZK_PATH_PREFIX] = prefix
        map[RpcConf.CLIENT_CHANNEL_NUMBER_PER_SERVER] = 1

        def c = new RpcConf()
        c.extend(map)

        def one = new RpcClient(c)
        log.info 'created client - ' + key
        cached[key] = one
        new RpcClientWapper(client: one, isFirstCreated: true)
    }

    void stop() {
        cached.each { k, v ->
            log.info 'ready to stop rpc client - ' + k
            v.stop()
            log.info 'ready to stop rpc client - ' + k
        }
    }
}
