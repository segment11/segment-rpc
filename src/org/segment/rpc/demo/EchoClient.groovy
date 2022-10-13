package org.segment.rpc.demo

import org.segment.rpc.client.RpcClient
import org.segment.rpc.common.Utils
import org.segment.rpc.invoke.ProxyCreator
import org.segment.rpc.server.handler.Req

def client = new RpcClient()

Utils.stopWhenConsoleQuit {
    client.stop()
}

// test method invoke
SayInterface say = new ProxyCreator(client, '/rpc').create(SayInterface)
println say.hi('kerry')
def resp = client.sendSync(new Req('/rpc/v1/echo', "hi kerry".toString()))
println '' + resp.status + ':' + resp?.body

int threadNumber = 10
int loopTimes = 10
threadNumber.times { i ->
    Thread.start {
        loopTimes.times { j ->
            def resp2 = client.sendSync(new Req('/rpc/v1/echo', "hi ${i}, ${j}".toString()))
            println '' + resp2.status + ':' + resp2?.body
            println say.hi("kerry ${i}, ${j}".toString())
            // mock do business
            long ms = 10 + new Random().nextInt(10)
            Thread.sleep(ms)
        }
    }
}
