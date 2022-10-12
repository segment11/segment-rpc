package org.segment.rpc.server.handler

import groovy.util.logging.Slf4j

import java.util.concurrent.CopyOnWriteArrayList
import java.util.regex.Pattern

@Singleton
@Slf4j
class ChainHandler implements Handler {
    @Override
    Resp handle(Req req) {
        try {
            handleListNoReturn(req, beforeList)
            def resp = handleList(req, list)
            handleListNoReturn(req, afterList)
            return resp
        } catch (HaltEx haltEx) {
            return Resp.fail(haltEx.message, haltEx.status)
        } catch (Throwable t) {
            if (exceptionHandler) {
                try {
                    return exceptionHandler.handle(req, t)
                } catch (Exception e2) {
                    log.error('exception handle error', e2)
                }
            } else {
                throw t
            }
        } finally {
            try {
                handleListNoReturn(req, afterAfterList)
            } catch (Exception e) {
                log.error('after after handle error', e)
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

    private String uriPre

    ChainHandler uriPre(String uriPre) {
        this.uriPre = uriPre
        this
    }

    private String addUriPre(String uri) {
        if (uriPre == null) {
            return uri
        }
        uriPre + uri
    }

    void group(String groupUri, Closure closure) {
        if (uriPre) {
            uriPre += groupUri
        } else {
            uriPre = groupUri
        }
        closure.call()
        uriPre = uriPre[0..-(groupUri.length() + 1)]
    }

    private ChainHandler add(String uri, AbstractHandler handler,
                             CopyOnWriteArrayList<Handler> ll) {
        handler.uri = addUriPre(uri)
        removeOneThatExists(handler, ll)
        ll << handler
        log.info 'add handler {}', handler.uri
        this
    }

    private ChainHandler addRegex(Pattern pattern, RegexMatchHandler handler,
                                  CopyOnWriteArrayList<Handler> ll) {
        handler.uriPre = uriPre
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
