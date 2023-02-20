package org.segment.rpc.server.serialize.compress

import groovy.transform.CompileStatic
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream
import org.segment.rpc.server.serialize.Compress

@CompileStatic
class Lz4Compress implements Compress {
    private static final int BUFFER_SIZE = 1024 * 4

    @Override
    void compress(InputStream is, OutputStream os) {
        def lz = new FramedLZ4CompressorOutputStream(os)
        try {
            byte[] buffer = new byte[BUFFER_SIZE]
            int n
            while (-1 != (n = is.read(buffer))) {
                lz.write(buffer, 0, n)
            }
            lz.flush()
            lz.finish()
        } catch (IOException e) {
            throw new RuntimeException('lz4 compress error', e)
        }
    }

    @Override
    void decompress(InputStream is, OutputStream os) {
        def lz = new FramedLZ4CompressorInputStream(is)
        try {
            byte[] buffer = new byte[BUFFER_SIZE]
            int n
            while ((n = lz.read(buffer)) > -1) {
                os.write(buffer, 0, n)
            }
            os.flush()
        } catch (IOException e) {
            throw new RuntimeException('lz4 decompress error', e)
        }
    }
}
