package org.segment.rpc.server.serialize.gzip

import groovy.transform.CompileStatic
import org.segment.rpc.server.serialize.Compress

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@CompileStatic
class GZipCompress implements Compress {
    private static final int BUFFER_SIZE = 1024 * 4

    @Override
    byte[] compress(byte[] data) {
        def out = new ByteArrayOutputStream()
        def gzip = new GZIPOutputStream(out)
        try {
            gzip.write(data)
            gzip.flush()
            gzip.finish()
            return out.toByteArray()
        } catch (IOException e) {
            throw new RuntimeException('gzip compress error', e)
        }
    }

    @Override
    byte[] decompress(byte[] data) {
        def out = new ByteArrayOutputStream()
        def gzip = new GZIPInputStream(new ByteArrayInputStream(data))
        try {
            byte[] buffer = new byte[BUFFER_SIZE]
            int n
            while ((n = gzip.read(buffer)) > -1) {
                out.write(buffer, 0, n)
            }
            return out.toByteArray()
        } catch (IOException e) {
            throw new RuntimeException('gzip decompress error', e)
        }
    }
}
