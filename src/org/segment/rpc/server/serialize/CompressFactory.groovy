package org.segment.rpc.server.serialize

import groovy.transform.CompileStatic
import org.segment.rpc.server.codec.RpcMessage
import org.segment.rpc.server.serialize.compress.GZipCompress
import org.segment.rpc.server.serialize.compress.Lz4Compress

@CompileStatic
class CompressFactory {
    static final boolean hasVendorCompress = isCompressVendor()

    static boolean isCompressVendor() {
        ServiceLoader.load(Compress.class).find { it.class.name.contains('vendor') } != null
    }

    static Compress create(RpcMessage.CompressType compressType) {
        if (compressType == RpcMessage.CompressType.NONE) {
            return null
        }

        if (compressType == RpcMessage.CompressType.GZIP) {
            return new GZipCompress()
        } else if (compressType == RpcMessage.CompressType.LZ4) {
            return new Lz4Compress()
        } else if (compressType == RpcMessage.CompressType.CUSTOM) {
            def compress = (Compress) ServiceLoader.load(Compress.class).find { it.class.name.contains('vendor') }
            if (compress == null) {
                throw new IllegalArgumentException("no custom compress found, " +
                        "need to implement interface Compress, package name contains 'vendor'")
            }
            return compress
        } else {
            throw new IllegalArgumentException("unknown compress type: " + compressType)
        }
    }
}
