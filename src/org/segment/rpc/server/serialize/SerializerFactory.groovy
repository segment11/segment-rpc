package org.segment.rpc.server.serialize

import groovy.transform.CompileStatic
import org.segment.rpc.server.serialize.hessian.HessianSerializer
import org.segment.rpc.server.serialize.kyro.KyroSerializer

@CompileStatic
class SerializerFactory {
    static Serializer create(Serializer.Type type) {
        if (type == Serializer.Type.KYRO) {
            return new KyroSerializer()
        }
        if (type == Serializer.Type.HESSIAN) {
            return new HessianSerializer()
        }

        return new KyroSerializer()
    }
}
