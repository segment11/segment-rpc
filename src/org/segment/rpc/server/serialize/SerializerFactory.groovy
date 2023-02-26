package org.segment.rpc.server.serialize

import groovy.transform.CompileStatic
import org.segment.rpc.server.serialize.hessian.HessianSerializer
import org.segment.rpc.server.serialize.kyro.KyroSerializer

@CompileStatic
class SerializerFactory {
    static final boolean hasVendorSerializer = isSerializerVendor()

    static boolean isSerializerVendor() {
        ServiceLoader.load(Serializer.class).find { it.class.name.contains('vendor') } != null
    }

    static Serializer create(Serializer.Type type) {
        if (type == Serializer.Type.KYRO) {
            return new KyroSerializer()
        } else if (type == Serializer.Type.HESSIAN) {
            return new HessianSerializer()
        } else if (type == Serializer.Type.CUSTOM) {
            def serializer = (Serializer) ServiceLoader.load(Serializer.class).
                    find { it.class.name.contains('vendor') }
            if (serializer == null) {
                throw new IllegalArgumentException("no custom serializer found, " +
                        "need to implement interface Serializer, package name contains 'vendor'")
            }
            return serializer
        }

        return new KyroSerializer()
    }
}
