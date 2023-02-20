package org.segment.rpc.server.serialize.compress

import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.serialize.kyro.KyroSerializer
import spock.lang.Specification

class Lz4CompressTest extends Specification {
    def testCompress() {
        given:
        def serializer = new KyroSerializer()
        def compress = new Lz4Compress()
        def req = new Req('/test', [key1: 'value1'])
        and:
        def os = new ByteArrayOutputStream()
        def pipeIn = new PipedInputStream()
        def pipeIn2 = new PipedInputStream()
        def pipeOut = new PipedOutputStream(pipeIn)
        def pipeOut2 = new PipedOutputStream(pipeIn2)

        serializer.write(req, pipeOut)
        compress.compress(pipeIn, os)

        def bytes = os.toByteArray()
        println "after compress bytes: ${bytes.length}"

        def is = new ByteArrayInputStream(bytes)
        compress.decompress(is, pipeOut2)
        def req2 = serializer.read(pipeIn2, Req)
        expect:
        req != req2
        req.uuid == req2.uuid
        req.uri == req2.uri
        req.body == req2.body
    }
}
