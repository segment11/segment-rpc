package org.segment.rpc.server.handler

import spock.lang.Specification

class AbstractHandlerTest extends Specification {
    def 'match'() {
        given:
        expect:
        new One('/a').isUriMatch('/a')
        new One('/a/*').isUriMatch('/a/b')
        new One('/a/**').isUriMatch('/a/b/c')
        new One('/a/b/c').isUriMatch('/a/b/c')
        new One('/a/b/:name').isUriMatch('/a/b/c')
    }

    class One extends AbstractHandler {
        One(String uri) {
            this.uri = uri
        }

        @Override
        Object hi(Req req) {
            return null
        }
    }
}
