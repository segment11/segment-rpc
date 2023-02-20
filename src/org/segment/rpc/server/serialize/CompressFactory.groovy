package org.segment.rpc.server.serialize

import groovy.transform.CompileStatic
import org.segment.rpc.server.codec.RpcMessage
import org.segment.rpc.server.serialize.compress.GZipCompress
import org.segment.rpc.server.serialize.compress.Lz4Compress

@CompileStatic
class CompressFactory {
    static Compress create(RpcMessage.CompressType compressType) {
        if (compressType == RpcMessage.CompressType.NONE) {
            return null
        }

        if (compressType == RpcMessage.CompressType.GZIP) {
            return new GZipCompress()
        } else if (compressType == RpcMessage.CompressType.LZ4) {
            return new Lz4Compress()
        } else {
            throw new IllegalArgumentException("unknown compress type: " + compressType)
        }
    }
}
