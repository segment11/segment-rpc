package org.segment.rpc.server.registry

import groovy.transform.CompileStatic
import org.segment.rpc.server.handler.Req

@CompileStatic
interface Registry {
    void init()

    void register(RemoteUrl url)

    List<RemoteUrl> discover(Req req)

    void shutdown()
}