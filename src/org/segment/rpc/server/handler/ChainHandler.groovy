package org.segment.rpc.server.handler

import groovy.util.logging.Slf4j
import io.prometheus.client.Gauge
import io.prometheus.client.Summary
import org.segment.rpc.server.registry.RemoteUrl
import org.segment.rpc.stats.CounterInMinute
import org.segment.rpc.stats.StatsType

import java.util.concurrent.CopyOnWriteArrayList
import java.util.regex.Pattern

@Singleton
@Slf4j
class ChainHandler implements Handler {
    private RemoteUrl remoteUrl

    void setRemoteUrl(RemoteUrl remoteUrl) {
        this.remoteUrl = remoteUrl
    }

    private String getAddress() {
        remoteUrl ? remoteUrl.toString() : ''
    }

    private Summary cost = Summary.build().name('handle_ms').
            help('rpc handler cost time').
            labelNames('address', 'context', 'uri').
            quantile(0.5.doubleValue(), 0.05.doubleValue()).
            quantile(0.9.doubleValue(), 0.01.doubleValue()).register()

    private Gauge handlerNumber = Gauge.build().name('handler_number').
            help('handler number').labelNames('address', 'context').register()

    @Override
    Resp handle(Req req) {
        Summary.Timer timer
        // method invoke use another timer
        if (!req.isMethodInvoke) {
            timer = cost.labels(getAddress(), req.context(), req.uri).startTimer()
        }
        try {
            handleListNoReturn(req, beforeList)
            def resp = handleList(req, list)
            handleListNoReturn(req, afterList)

            if (resp == null || resp.status == Resp.Status.NOT_FOUND) {
                CounterInMinute.instance.increaseAndGet(1, StatsType.RESP_404)
            }
            if (resp != null && resp.status == Resp.Status.INTERNAL_EX) {
                CounterInMinute.instance.increaseAndGet(1, StatsType.RESP_500)
            }
            return resp
        } catch (HaltEx haltEx) {
            CounterInMinute.instance.increaseAndGet(1, StatsType.RESP_500)
            return Resp.fail(haltEx.message, haltEx.status)
        } catch (Throwable t) {
            CounterInMinute.instance.increaseAndGet(1, StatsType.RESP_500)
            if (exceptionHandler) {
                try {
                    return exceptionHandler.handle(req, t)
                } catch (Exception e2) {
                    log.error('exception handle error', e2)
                    return Resp.fail('exception handle error', e2.message)
                }
            } else {
                log.error('exception handle error', t)
                return Resp.fail('exception handle error', t.message)
            }
        } finally {
            try {
                handleListNoReturn(req, afterAfterList)
            } catch (Exception e) {
                log.error('after after handle error', e)
            }
            if (timer) {
                timer.observeDuration()
            }
        }
    }

    private void handleListNoReturn(Req req, List<Handler> ll) {
        for (handler in ll) {
            handler.handle(req)
        }
    }

    private Resp handleList(Req req, List<Handler> ll) {
        if (!ll.size()) {
            return null
        }

        for (handler in ll) {
            def r = handler.handle(req)
            if (r) {
                return r
            }
        }
        null
    }

    @Override
    String name() {
        'chain'
    }

    CopyOnWriteArrayList<Handler> list = new CopyOnWriteArrayList<>()
    CopyOnWriteArrayList<Handler> beforeList = new CopyOnWriteArrayList<>()
    CopyOnWriteArrayList<Handler> afterList = new CopyOnWriteArrayList<>()
    CopyOnWriteArrayList<Handler> afterAfterList = new CopyOnWriteArrayList<>()

    void print(Closure closure) {
        closure.call('list: ' + list.collect { it.name() })
        closure.call('beforeList: ' + beforeList.collect { it.name() })
        closure.call('afterList: ' + afterList.collect { it.name() })
        closure.call('afterAfterList: ' + afterAfterList.collect { it.name() })
    }

    private ExceptionHandler exceptionHandler

    ChainHandler exceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler
        this
    }

    private void removeOneThatExists(Handler handler, CopyOnWriteArrayList<Handler> ll = null) {
        def r = ll == null ? list : ll
        def one = r.find { it.name() == handler.name() }
        if (one) {
            r.remove(one)
        }
    }

    private String context

    ChainHandler context(String context) {
        this.context = context
        this
    }

    private String addContextPath(String uri) {
        if (context == null) {
            return uri
        }
        context + uri
    }

    void group(String groupUri, Closure closure) {
        if (context) {
            context += groupUri
        } else {
            context = groupUri
        }
        closure.call()
        context = context[0..-(groupUri.length() + 1)]
    }

    private ChainHandler add(String uri, AbstractHandler handler,
                             CopyOnWriteArrayList<Handler> ll) {
        handler.uri = addContextPath(uri)
        removeOneThatExists(handler, ll)
        ll << handler
        log.info 'add handler {}', handler.uri
        handlerNumber.labels(getAddress(), remoteUrl.context).set(ll.size() as double)
        this
    }

    private ChainHandler addRegex(Pattern pattern, RegexMatchHandler handler,
                                  CopyOnWriteArrayList<Handler> ll) {
        handler.context = context
        handler.pattern = pattern
        removeOneThatExists(handler, ll)
        ll << handler
        this
    }

    ChainHandler get(String uri, AbstractHandler handler) {
        add(uri, handler, list)
    }

    ChainHandler get(Pattern pattern, RegexMatchHandler handler) {
        addRegex(pattern, handler, list)
    }

    ChainHandler before(String uri, AbstractHandler handler) {
        add(uri, handler, beforeList)
    }

    ChainHandler before(Pattern pattern, RegexMatchHandler handler) {
        addRegex(pattern, handler, beforeList)
    }

    ChainHandler after(String uri, AbstractHandler handler) {
        add(uri, handler, afterList)
    }

    ChainHandler after(Pattern pattern, RegexMatchHandler handler) {
        addRegex(pattern, handler, afterList)
    }

    ChainHandler afterAfter(String uri, AbstractHandler handler) {
        add(uri, handler, afterAfterList)
    }

    ChainHandler afterAfter(Pattern pattern, RegexMatchHandler handler) {
        addRegex(pattern, handler, afterAfterList)
    }
}
