package org.segment.rpc.server.serialize.kyro

import org.segment.rpc.server.handler.Req
import spock.lang.Specification

class KyroSerializerTest extends Specification {
    void testReadWrite() {
        given:
        def serializer = new KyroSerializer()
        def req = new Req('/test', [key1: 'value1'])
        def os = new ByteArrayOutputStream()
        and:
        serializer.write(req, os)
        def is = new ByteArrayInputStream(os.toByteArray())
        def req2 = serializer.read(is, Req)
        expect:
        req != req2
        req.uuid == req2.uuid
        req.uri == req2.uri
        req.body == req2.body
    }
}
