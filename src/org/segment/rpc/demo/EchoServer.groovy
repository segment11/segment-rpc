package org.segment.rpc.demo

import org.segment.rpc.common.Conf
import org.segment.rpc.common.Utils
import org.segment.rpc.server.RpcServer
import org.segment.rpc.server.handler.ChainHandler
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.provider.DefaultProvider
import org.segment.rpc.server.registry.local.LocalRegistry

Conf c = Conf.instance
c.load()
if (c.isOn('registry.use.local')) {
    // server need start h2 server first if use local registry
    LocalRegistry.instance.isNeedStartH2Server = true
    LocalRegistry.instance.isNeedClearTableFirst = true
}

def h = ChainHandler.instance
h.uriPre('/rpc').group('/v1') {
    h.get('/echo') { req ->
        println req.body
        Resp.one('ok - ' + req.body)
    }
}

DefaultProvider.instance.provide(SayInterface.class, new SayImpl())

def server = new RpcServer()
Utils.stopWhenConsoleQuit {
    server.stop()
}
server.start()