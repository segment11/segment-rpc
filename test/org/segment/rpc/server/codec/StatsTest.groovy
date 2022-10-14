package org.segment.rpc.server.codec

import spock.lang.Specification

class StatsTest extends Specification {

    def testGetLatestMin() {
        given:
        def stats = new Stats()
        10.times {
            stats.add(1000)
        }
        expect:
        stats.latestMin == 10000
    }

}
