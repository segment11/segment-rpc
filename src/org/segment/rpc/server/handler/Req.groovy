package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

@CompileStatic
class Req extends HeaderSupport implements Serializable {

    String uuid = UUID.randomUUID().toString()

    String uri
    Object body

    boolean isMethodInvoke = false

    Req() {}

    Req(String uri, Object body = null) {
        this.uri = uri
        this.body = body
    }

    private String contextCached

    // /rpc/a/b -> /rpc
    String context() {
        if (!uri) {
            return null
        }

        if (contextCached) {
            return contextCached
        }

        def arr = uri.split('/')
        if (arr.length > 1) {
            contextCached = '/' + arr[1]
            return contextCached
        }

        null
    }
}
