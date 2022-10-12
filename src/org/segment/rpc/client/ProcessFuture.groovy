package org.segment.rpc.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.server.handler.Resp

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Singleton
@Slf4j
class ProcessFuture {
    private final Map<String, CompletableFuture<Resp>> items = new ConcurrentHashMap<>()

    void put(String requestUuid, CompletableFuture<Resp> item) {
        items[requestUuid] = item
    }

    void complete(Resp resp) {
        def future = items.remove(resp.uuid)
        if (!future) {
            throw new IllegalStateException('resp get no process future as uuid: ' + resp.uuid)
        }
        future.complete(resp)
    }

    int pendingLength() {
        items.size()
    }
}
