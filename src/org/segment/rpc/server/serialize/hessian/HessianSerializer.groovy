package org.segment.rpc.server.serialize.hessian

import groovy.transform.CompileStatic
import org.segment.rpc.server.serialize.Serializer

@CompileStatic
class HessianSerializer implements Serializer {

    @Override
    def <T> T read(InputStream is, Class<T> clz) {
        return null
    }

    @Override
    int write(Object obj, OutputStream os) {

    }
}
