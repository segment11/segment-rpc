package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

@CompileStatic
interface ExceptionHandler {
    Resp handle(Req req, Throwable t)
}