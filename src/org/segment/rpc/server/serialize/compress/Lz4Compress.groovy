package org.segment.rpc.server.serialize.compress

import groovy.transform.CompileStatic
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream
import org.segment.rpc.server.serialize.Compress

@CompileStatic
class Lz4Compress implements Compress {
    private static final int BUFFER_SIZE = 1024 * 4

    @Override
    byte[] compress(byte[] data) {
        def out = new ByteArrayOutputStream()
        def lz = new FramedLZ4CompressorOutputStream(out)
        def is = new ByteArrayInputStream(data)

        try {
            byte[] buffer = new byte[BUFFER_SIZE]
            int n
            while (-1 != (n = is.read(buffer))) {
                lz.write(buffer, 0, n)
            }
            lz.flush()
            lz.finish()
            return out.toByteArray()
        } catch (IOException e) {
            throw new RuntimeException('lz4 compress error', e)
        }
    }

    @Override
    byte[] decompress(byte[] data) {
        def out = new ByteArrayOutputStream()
        def lz = new FramedLZ4CompressorInputStream(new ByteArrayInputStream(data))
        try {
            byte[] buffer = new byte[BUFFER_SIZE]
            int n
            while ((n = lz.read(buffer)) > -1) {
                out.write(buffer, 0, n)
            }
            return out.toByteArray()
        } catch (IOException e) {
            throw new RuntimeException('lz4 decompress error', e)
        }
    }
}
