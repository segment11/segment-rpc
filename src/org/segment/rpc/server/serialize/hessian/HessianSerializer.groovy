package org.segment.rpc.server.serialize.hessian

import groovy.transform.CompileStatic
import org.segment.rpc.server.serialize.Serializer

@CompileStatic
class HessianSerializer implements Serializer {
    @Override
    def <T> T read(byte[] data, Class<T> clz) {
        // todo
        return null
    }

    @Override
    byte[] write(Object obj) {
        return new byte[0]
    }
}
