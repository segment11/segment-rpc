package org.segment.rpc.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.registry.Referer
import org.segment.rpc.server.registry.RemoteUrl

@CompileStatic
@Singleton
@Slf4j
class DefaultLoadBalance implements LoadBalance {
    @Override
    Referer select(List<RemoteUrl> list, Req req) {
        // todo
        new Referer(remoteUrl: list[0])
    }

    @Override
    void init() {

    }

    @Override
    void shutdown() {

    }
}
