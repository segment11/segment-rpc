package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

@CompileStatic
class HaltEx extends RuntimeException {
    Resp.Status status = Resp.Status.INTERNAL_EX

    HaltEx(Resp.Status status, String message) {
        super(message)
        this.status = status
    }

    HaltEx(Resp.Status status, String message, Throwable cause) {
        super(message, cause)
        this.status = status
    }
}
