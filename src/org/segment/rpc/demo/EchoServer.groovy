package org.segment.rpc.demo

import org.segment.rpc.common.Utils
import org.segment.rpc.server.RpcServer
import org.segment.rpc.server.handler.ChainHandler
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.provider.DefaultProvider

def server = new RpcServer()

def h = ChainHandler.instance
h.context('/rpc').group('/v1') {
    h.get('/echo') { req ->
        println req.body
        Resp.one('ok - ' + req.body)
    }
}

DefaultProvider.instance.provide(SayInterface.class, new SayImpl())

Utils.stopWhenConsoleQuit {
    server.stop()
}
server.start()