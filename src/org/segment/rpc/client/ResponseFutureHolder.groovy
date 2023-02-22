package org.segment.rpc.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.common.RpcConf
import org.segment.rpc.server.handler.Resp

import java.util.concurrent.*

@CompileStatic
@Slf4j
class ResponseFutureHolder {
    private final Map<String, FutureWrapper> items = new ConcurrentHashMap<>()
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor()

    void init(RpcConf c) {
        int interval = c.getInt('client.response.future.check.timeout.interval.second', 10)
        def now = new Date()
        int sec = now.seconds
        long delaySeconds = interval - (sec % interval)

        scheduler.scheduleWithFixedDelay({
            // clear those timeout
            def current = System.currentTimeMillis()
            long buffer = 1000
            try {
                items.each { k, v ->
                    boolean isTimeout = (v.createdTime + v.timeoutMillis + buffer) < current
                    if (isTimeout) {
                        // will not happen, as it is already removed in RpcClient.sendSync
                        // unless debug cost too long time
                        log.warn 'why here response future timeout? uuid: {}', k
                        items.remove(k)
                        v.future.completeExceptionally(new TimeoutException())
                    }
                }
            } catch (Exception e) {
                log.error('response future clean error', e)
            }
        }, delaySeconds, interval, TimeUnit.SECONDS)
        log.info 'start response future holder interval check, interval: {}s', interval
    }

    void stop() {
        scheduler.shutdown()
        log.info 'stop interval check response future holder'

        def t = new IllegalStateException('discard')
        items.each { k, v ->
            v.future.completeExceptionally(t)
            log.warn 'discard request uuid : ' + k
        }
        items.clear()
    }

    void put(String requestUuid, CompletableFuture<Resp> item, int timeoutMillis) {
        items[requestUuid] = new FutureWrapper(item, timeoutMillis)
    }

    void remove(String requestUuid) {
        def wrapper = items.remove(requestUuid)
        if (wrapper) {
            def t = new IllegalStateException('discard')
            wrapper.future.completeExceptionally(t)
        }
    }

    void complete(Resp resp) {
        def uuid = resp.uuid
        // never happen
        if (!uuid) {
            log.warn 'resp has not uuid, status: {}, message: {} ', resp.status, resp.message
            return
        }

        def wrapper = items.remove(uuid)
        if (!wrapper) {
            // timeout removed already
            log.warn('resp get no process future, timeout? uuid: {}', uuid)
//            throw new IllegalStateException('resp get no process future, timeout? uuid: ' + resp.uuid)
            return
        }
        wrapper.future.complete(resp)
    }

    int size() {
        items.size()
    }

    @CompileStatic
    class FutureWrapper {
        CompletableFuture<Resp> future

        int timeoutMillis

        long createdTime

        FutureWrapper(CompletableFuture<Resp> future, int timeoutMillis) {
            this.future = future
            this.timeoutMillis = timeoutMillis
            this.createdTime = System.currentTimeMillis()
        }
    }
}
