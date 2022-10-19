package org.segment.rpc.client

import org.segment.rpc.client.loadbalance.RandomLoadBalance
import org.segment.rpc.server.registry.RemoteUrl
import spock.lang.Specification

class RandomLoadBalanceTest extends Specification {
    void testSelect() {
        given:
        def loadBalance = RandomLoadBalance.instance
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
        def remoteUrl = loadBalance.select(list, null)
        println remoteUrl.toStringView()
        expect:
        remoteUrl.weight >= 10
    }
}
