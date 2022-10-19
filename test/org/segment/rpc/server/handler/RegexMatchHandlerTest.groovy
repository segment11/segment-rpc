package org.segment.rpc.server.handler

import spock.lang.Specification

class RegexMatchHandlerTest extends Specification {
    def 'match'() {
        given:
        def handler = new RegexMatchHandler() {
            @Override
            Object hi(Req req) {
                return null
            }
        }
        handler.context = '/a'
        handler.pattern = ~/^\/b\/.+$/
        expect:
        handler.isUriMatch('/a/b/c')
    }
}
