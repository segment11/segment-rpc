package org.segment.rpc.server.codec

import spock.lang.Specification

class LimitBlockQueueTest extends Specification {
    void testOffer() {
        given:
        def q = new LimitBlockQueue<Integer>(100)
        and:
        101.times {
            q.offer(it)
        }
        expect:
        q.size() == 100
        q[0] == 1
    }
}
