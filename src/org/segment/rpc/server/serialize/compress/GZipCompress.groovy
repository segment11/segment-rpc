package org.segment.rpc.server.serialize.compress

import groovy.transform.CompileStatic
import org.segment.rpc.server.serialize.Compress

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@CompileStatic
class GZipCompress implements Compress {
    private static final int BUFFER_SIZE = 1024 * 4

    @Override
    void compress(InputStream is, OutputStream os) {
        def gzip = new GZIPOutputStream(os)
        try {
            byte[] buffer = new byte[BUFFER_SIZE]
            int n
            while (-1 != (n = is.read(buffer))) {
                gzip.write(buffer, 0, n)
            }
            gzip.flush()
            gzip.finish()
        } catch (IOException e) {
            throw new RuntimeException('gzip compress error', e)
        }
    }

    @Override
    void decompress(InputStream is, OutputStream os) {
        def gzip = new GZIPInputStream(is)
        try {
            byte[] buffer = new byte[BUFFER_SIZE]
            int n
            while ((n = gzip.read(buffer)) > -1) {
                os.write(buffer, 0, n)
            }
            os.flush()
        } catch (IOException e) {
            throw new RuntimeException('gzip decompress error', e)
        }
    }
}
