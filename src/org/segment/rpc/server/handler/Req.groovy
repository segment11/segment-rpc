package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
class Req extends HeaderSupport implements Serializable {
    static final AtomicInteger x = new AtomicInteger(0)

    String uuid = System.currentTimeMillis().toString()[0..-4] + '_' + x.incrementAndGet()

    String uri

    Object body

    int retries = 0

    boolean needRetry = false

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
