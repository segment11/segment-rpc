package org.segment.rpc.invoke

import groovy.transform.CompileStatic

@CompileStatic
class RpcMethodInvokeException extends Exception {
    RpcMethodInvokeException(String s) {
        super(s)
    }
}
