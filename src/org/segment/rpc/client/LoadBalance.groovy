package org.segment.rpc.client

import groovy.transform.CompileStatic
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.registry.Referer
import org.segment.rpc.server.registry.RemoteUrl

@CompileStatic
interface LoadBalance {
    Referer select(List<RemoteUrl> list, Req req)
}
