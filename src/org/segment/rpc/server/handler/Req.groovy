package org.segment.rpc.server.handler

import groovy.transform.CompileStatic
import org.segment.rpc.server.serialize.Compress
import org.segment.rpc.server.serialize.Serializer

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
class Req extends HeaderSupport implements Serializable {
    static final AtomicInteger x = new AtomicInteger(0)

    String uuid = System.currentTimeMillis().toString()[0..-4] + '_' + x.incrementAndGet()

    String uri

    Object body

    int thisRetryTime = 0

    boolean needRetry = false

    boolean isMethodInvoke = false

    Compress.Type compressType
    Serializer.Type serializeType

    Req lz4() {
        compressType = Compress.Type.LZ4
        this
    }

    Req gzip() {
        compressType = Compress.Type.GZIP
        this
    }

    Req kryo() {
        serializeType = Serializer.Type.KYRO
        this
    }

    Req hessian() {
        serializeType = Serializer.Type.HESSIAN
        this
    }

    // only for decode
    Req() {}

    Req(String uri, Object body = null) {
        if (uri == null) {
            throw new IllegalArgumentException('uri is null')
        }
        if (!uri.startsWith('/')) {
            throw new IllegalArgumentException('uri must start with /')
        }
        this.uri = uri
        this.body = body
    }

    private String contextCached

    // /rpc/a/b -> /rpc
    String context() {
        if (contextCached) {
            return contextCached
        }

        if (uri == '/') {
            return '/'
        }

        def arr = uri.split('/')
        contextCached = '/' + arr[1]
        return contextCached
    }
}
