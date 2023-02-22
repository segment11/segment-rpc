package org.segment.rpc.server.handler

import spock.lang.Specification

class ReqTest extends Specification {
    def "Context"() {
        given:
        def req = new Req('/')
        def req1 = new Req('/rpc')
        def req2 = new Req('/rpc/a')
        def req3 = new Req('/rpc/a/b')
        expect:
        req.context() == '/'
        req1.context() == '/rpc'
        req2.context() == '/rpc'
        req3.context() == '/rpc'
    }
}
