package org.segment.rpc.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.registry.Referer
import org.segment.rpc.server.registry.RemoteUrl

@CompileStatic
@Singleton
@Slf4j
class DefaultLoadBalance implements LoadBalance {
    @Override
    Referer select(List<RemoteUrl> list, Req req) {
        int sum = 0
        for (int i = 0; i < list.size(); i++) {
            sum += list[i].weight
        }

        RemoteUrl remoteUrl
        if (sum < RemoteUrl.DEFAULT_WEIGHT) {
            if (list) {
                remoteUrl = list[0]
            }
        } else {
            int weight = new Random().nextInt(sum)
            int sumInner = 0
            for (int i = 0; i < list.size(); i++) {
                sumInner += list[i].weight
                if (sumInner > weight) {
                    remoteUrl = list[i]
                    break
                }
            }
        }

        if (!remoteUrl) {
            return null
        }
        new Referer(remoteUrl: remoteUrl)
    }

    @Override
    void init() {

    }

    @Override
    void shutdown() {

    }
}
