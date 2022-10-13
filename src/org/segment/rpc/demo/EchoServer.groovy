package org.segment.rpc.demo

import org.segment.rpc.common.Conf
import org.segment.rpc.common.Utils
import org.segment.rpc.invoke.NameMappingLoaderByInterface
import org.segment.rpc.server.RpcServer
import org.segment.rpc.server.handler.ChainHandler
import org.segment.rpc.server.handler.Resp
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
h.print {
    println it
}

// for test method invoke
String impl =
        '''
org.segment.rpc.demo.SayInterface=org.segment.rpc.demo.SayImpl
'''
impl.trim().readLines().each {
    def line = it.trim()
    if (!line || line.startsWith('#')) {
        return
    }
    def arr = line.split('=')
    NameMappingLoaderByInterface.instance.put(arr[0], arr[1])
}

def server = new RpcServer()
h.print {
    println it
}

Utils.stopWhenConsoleQuit {
    server.stop()
}

server.start()