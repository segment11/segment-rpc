package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

@CompileStatic
interface Handler {
    Resp handle(Req req)

    String name()
}