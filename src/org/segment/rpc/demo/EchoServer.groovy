package org.segment.rpc.demo

import org.segment.rpc.common.ConsoleReader
import org.segment.rpc.invoke.MethodMeta
import org.segment.rpc.invoke.ProxyCreator
import org.segment.rpc.server.RpcServer
import org.segment.rpc.server.handler.ChainHandler
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.provider.DefaultProvider
import org.slf4j.LoggerFactory

boolean isEchoSleep = false
boolean isServerStopAfterSomeTime = false
boolean isDoLog = false

def log = LoggerFactory.getLogger(this.getClass())
def server = new RpcServer()

def h = ChainHandler.instance
h.context('/rpc').group('/v1') {
    h.get('/echo') { req ->
        if (isDoLog) {
            log.info req.body
        }

        if (isEchoSleep) {
            long ms = 10 + new Random().nextInt(10)
            Thread.currentThread().sleep(ms)
        }
        1
    }.get('/ex') { req ->
        Resp.fail('error')
    }.get('/timeout') { req ->
        if (req.thisRetryTime == 0) {
            Thread.sleep(3000)
        }
        1
    }
}

DefaultProvider.instance.provide(SayInterface.class, new SayImpl())

h.before(ProxyCreator.HANDLER_URI) { req ->
    MethodMeta meta = req.body as MethodMeta
    log.info meta.toString()
    null
}

def reader = ConsoleReader.instance
reader.quitHandler = {
    println 'stop...'
    server.beforeStop()
    server.stop()
}
reader.read()

if (isServerStopAfterSomeTime) {
    Thread.start {
        Thread.sleep(100000)
        reader.stop()
//        server.beforeStop()
        server.stop()
    }
}

server.start()