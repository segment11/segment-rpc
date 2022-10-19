package org.segment.rpc.demo

import org.segment.rpc.common.ConsoleReader
import org.segment.rpc.server.RpcServer
import org.segment.rpc.server.handler.ChainHandler
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.provider.DefaultProvider
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(this.getClass())
def server = new RpcServer()

def h = ChainHandler.instance
h.context('/rpc').group('/v1') {
    h.get('/echo') { req ->
        log.info req.body

        long ms = 10 + new Random().nextInt(50)
        Thread.currentThread().sleep(ms)
        1
    }.get('/ex') { req ->
        Resp.fail('error')
    }.get('/timeout') { req ->
//        if (req.retries == 0) {
            Thread.sleep(3000)
//        }
        1
    }
}

DefaultProvider.instance.provide(SayInterface.class, new SayImpl())

def reader = ConsoleReader.instance
reader.quitHandler = {
    println 'stop...'
    server.stop()
}
reader.read()

Thread.start {
    // mock server down normally
    Thread.sleep(10000)
    reader.stop()
    server.stop()
}

server.start()