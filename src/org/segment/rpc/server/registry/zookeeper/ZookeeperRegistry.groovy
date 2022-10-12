package org.segment.rpc.server.registry.zookeeper

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.registry.Registry
import org.segment.rpc.server.registry.RemoteUrl

@CompileStatic
@Singleton
@Slf4j
class ZookeeperRegistry implements Registry {
    @Override
    void register(RemoteUrl url) {

    }

    @Override
    List<RemoteUrl> discover(Req req) {
        // todo
        List<RemoteUrl> list = []
        list << new RemoteUrl(host: '192.168.99.1', port: 8877)
        list
    }

    @Override
    void shutdown() {

    }
}
