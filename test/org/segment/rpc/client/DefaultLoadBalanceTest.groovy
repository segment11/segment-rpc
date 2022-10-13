package org.segment.rpc.client

import org.segment.rpc.server.registry.RemoteUrl
import spock.lang.Specification

class DefaultLoadBalanceTest extends Specification {
    void testSelect() {
        given:
        def loadBalance = DefaultLoadBalance.instance
        List<RemoteUrl> list = []
        and:
        5.times {
            list << new RemoteUrl('', 0)
        }
        5.times {
            def one = new RemoteUrl('', 0)
            one.weight = 20
            list << one
        }
        def one = loadBalance.select(list, null)
        println one.remoteUrl.toStringView()
        expect:
        one.remoteUrl.weight >= 10
    }
}
