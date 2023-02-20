package org.segment.rpc.manage

import groovy.transform.CompileStatic
import org.segment.rpc.client.RpcClient

@CompileStatic
class RpcClientWrapper {
    RpcClient client

    boolean isFirstCreated = false
}
