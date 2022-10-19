package org.segment.rpc.client.loadbalance

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.registry.RemoteUrl

@CompileStatic
@Singleton
@Slf4j
class RandomLoadBalance implements LoadBalance {
    @Override
    RemoteUrl select(List<RemoteUrl> list, Req req) {
        if (!list) {
            return null
        }

        int sum = 0
        for (int i = 0; i < list.size(); i++) {
            sum += list[i].weight
        }

        int weight = new Random().nextInt(sum)
        int sumInner = 0
        for (int i = 0; i < list.size(); i++) {
            sumInner += list[i].weight
            if (sumInner > weight) {
                return list[i]
            }
        }
        list[-1]
    }

    @Override
    void init() {

    }

    @Override
    void shutdown() {

    }
}
