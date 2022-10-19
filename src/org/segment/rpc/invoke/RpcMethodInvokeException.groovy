package org.segment.rpc.invoke

import groovy.transform.CompileStatic

@CompileStatic
class RpcMethodInvokeException extends RuntimeException {
    RpcMethodInvokeException(String s) {
        super(s)
    }
}
