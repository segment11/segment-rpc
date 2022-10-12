package org.segment.rpc.server.serialize

import groovy.transform.CompileStatic

@CompileStatic
interface Serializer {
    public <T> T read(byte[] data, Class<T> clz)

    byte[] write(Object obj)

    static enum Type {
        KYRO(1 as Byte), HESSIAN(2 as Byte)

        byte value

        Type(byte value) {
            this.value = value
        }

        static Type convert(byte b) {
            if (b == 1) {
                return KYRO
            }

            if (b == 2) {
                return HESSIAN
            }
        }
    }
}