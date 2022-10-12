package org.segment.rpc.server.serialize

import groovy.transform.CompileStatic
import org.segment.rpc.server.codec.RpcMessage
import org.segment.rpc.server.serialize.gzip.GZipCompress

@CompileStatic
class CompressFactory {
    static Compress create(RpcMessage.CompressType compressType) {
        if (compressType == RpcMessage.CompressType.GZIP) {
            return new GZipCompress()
        }

        return new GZipCompress()
    }
}
