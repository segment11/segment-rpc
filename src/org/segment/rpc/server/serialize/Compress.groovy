package org.segment.rpc.server.serialize

import groovy.transform.CompileStatic

@CompileStatic
interface Compress {
    void compress(InputStream is, OutputStream os)

    void decompress(InputStream is, OutputStream os)

    @CompileStatic
    static enum Type {
        NONE(1 as Byte), GZIP(2 as Byte), LZ4(3 as Byte), CUSTOM(4 as Byte)

        byte value

        Type(byte value) {
            this.value = value
        }

        static Type convert(byte b) {
            if (b == 1) {
                return NONE
            }

            if (b == 2) {
                return GZIP
            }

            if (b == 3) {
                return LZ4
            }

            if (b == 4) {
                return CUSTOM
            }
        }
    }
}