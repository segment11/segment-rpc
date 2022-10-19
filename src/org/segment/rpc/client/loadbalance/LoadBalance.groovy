package org.segment.rpc.client.loadbalance

import groovy.transform.CompileStatic
import org.segment.rpc.common.RpcConf
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.registry.RemoteUrl

@CompileStatic
interface LoadBalance {
    RemoteUrl select(List<RemoteUrl> list, Req req)

    void init(RpcConf c)

    void shutdown()
}
