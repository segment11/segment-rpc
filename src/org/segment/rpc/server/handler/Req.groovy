package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

@CompileStatic
class Req extends HeaderSupport implements Serializable {

    String uuid = UUID.randomUUID().toString()

    String uri
    Object body

    Req() {}

    Req(String uri, Object body = null) {
        this.uri = uri
        this.body = body
    }
}
