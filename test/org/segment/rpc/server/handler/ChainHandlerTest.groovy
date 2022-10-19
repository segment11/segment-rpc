package org.segment.rpc.server.handler

import spock.lang.Specification

class ChainHandlerTest extends Specification {
    def 'server'() {
        given:
        def handler = ChainHandler.instance
        handler.exceptionHandler { req, t ->
            Resp.fail('error - ' + t.message)
        }
        handler.group('/a') {
            handler.group('/b') {
                handler.before('/c') { req ->
                    println 'before c filter'
                }.get('/c') { req ->
                    'get c'
                }.get('/d') { req ->
                    'get d'
                }.after('/c') { req ->
                    println 'after c filter'
                }.get('/map') { req ->
                    [1: 1]
                }
            }
            handler.group('/test') {
                handler.get('/halt') { req ->
                    Resp.fail('halt')
                }.get('/exception') { req ->
                    throw new RuntimeException('xxx')
                }.afterAfter('/exception') { req ->
                    println 'after exception'
                }
            }
            handler.group('/regex') {
                handler.get(~/^\/book.+$/) { req ->
                    'get book'
                }
            }
        }
        and:
        handler.print { String x ->
            println x
        }
        expect:
        handler.handle(new Req('/a/b/c')).body == 'get c'
        handler.handle(new Req('/a/b/d')).body == 'get d'
        handler.handle(new Req('/a/b/map')).body == [1: 1]
        !handler.handle(new Req('/a/test/halt')).ok()
        !handler.handle(new Req('/a/test/exception')).ok()
        handler.handle(new Req('/a/regex/book1')).body == 'get book'
    }
}
