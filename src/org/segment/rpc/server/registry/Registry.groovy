package org.segment.rpc.server.registry

import groovy.transform.CompileStatic
import org.segment.rpc.common.RpcConf
import org.segment.rpc.server.handler.Req

@CompileStatic
interface Registry {
    void init(RpcConf c)

    void register(RemoteUrl url)

    void unavailable(RemoteUrl url)

    List<RemoteUrl> discover(Req req)

    void shutdown()
}