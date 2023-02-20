package org.segment.rpc.server.serialize.compress

import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.serialize.kyro.KyroSerializer
import spock.lang.Specification

class GZipCompressTest extends Specification {
    def testCompress() {
        given:
        def serializer = new KyroSerializer()
        def compress = new GZipCompress()
        def req = new Req('/test', [key1: 'value1'])
        and:
        def bytes = serializer.write(req)
        def bytesCompressed = compress.compress(bytes)
        def bytesDecompressed = compress.decompress(bytesCompressed)
        def req2 = serializer.read(bytes, Req)
        def req3 = serializer.read(bytesDecompressed, Req)
        expect:
        req != req2
        req.uuid == req2.uuid
        req.uri == req2.uri
        req.body == req2.body

        req != req3
        req.uuid == req3.uuid
        req.uri == req3.uri
        req.body == req3.body
    }
}
