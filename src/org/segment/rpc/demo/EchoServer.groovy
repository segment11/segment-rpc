package org.segment.rpc.demo

import org.segment.rpc.common.ConsoleReader
import org.segment.rpc.server.RpcServer
import org.segment.rpc.server.handler.ChainHandler
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.provider.DefaultProvider
import org.slf4j.LoggerFactory

def server = new RpcServer()

def log = LoggerFactory.getLogger(this.getClass())

def h = ChainHandler.instance
h.context('/rpc').group('/v1') {
    h.get('/echo') { req ->
        log.info 'request get ' + req.body

        long ms = 10 + new Random().nextInt(50)
        Thread.currentThread().sleep(ms)
        Resp.one('ok - ' + req.body)
    }.get('/ex') { req ->
        Resp.fail('error')
    }
}

DefaultProvider.instance.provide(SayInterface.class, new SayImpl())

def reader = ConsoleReader.instance
reader.quitHandler = {
    println 'stop...'
    server.stop()
}
reader.read()

server.start()