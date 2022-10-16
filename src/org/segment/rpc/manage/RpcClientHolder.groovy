package org.segment.rpc.manage


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.client.RpcClient
import org.segment.rpc.common.RpcConf

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Singleton
@Slf4j
class RpcClientHolder {
    private Map<String, RpcClient> cached = new ConcurrentHashMap<>()

    synchronized RpcClient create(String connectString, String prefix) {
        final String key = connectString + prefix
        def client = cached[key]
        if (client) {
            return client
        }

        Map<String, Object> map = [:]
        map[RpcConf.ZK_CONNECT_STRING] = connectString
        map[RpcConf.ZK_PATH_PREFIX] = prefix
        
        def c = new RpcConf()
        c.extend(map)

        def one = new RpcClient(c)
        log.info 'created client - ' + key
        cached[key] = one
        one
    }

    void stop() {
        cached.each { k, v ->
            log.info 'ready to stop rpc client - ' + k
            v.stop()
            log.info 'ready to stop rpc client - ' + k
        }
    }
}
