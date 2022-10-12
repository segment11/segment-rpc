# segment-rpc
A Java simple RPC framework based on netty writen in Groovy. Use routers instead of method invoke.

## TODO
> 1. registry by zookeeper
> 2. load balance and retry
> 3. metrics / dashboard

## Echo Client-Server Demo

### EchoServer.groovy
```groovy
package org.segment.rpc.demo

import org.segment.rpc.common.Utils
import org.segment.rpc.server.RpcServer
import org.segment.rpc.server.handler.ChainHandler
import org.segment.rpc.server.handler.Resp

def h = ChainHandler.instance
h.uriPre('/rpc').group('/v1') {
    h.get('/echo') { req ->
        println req.body
        Resp.one('ok - ' + req.body)
    }
}

def server = new RpcServer()

Utils.stopWhenConsoleQuit {
    server.stop()
}

server.start()
```

### EchoClient.groovy
```groovy
package org.segment.rpc.demo

import org.segment.rpc.client.RpcClient
import org.segment.rpc.common.Utils
import org.segment.rpc.server.handler.Req

def client = new RpcClient()

Utils.stopWhenConsoleQuit {
    client.stop()
}

10.times { i ->
    Thread.start {
        100.times { j ->
            println client.send(new Req('/rpc/v1/echo', "hi ${i}, ${j}".toString())).get()?.body
            // mock do business
            long ms = 10 + new Random().nextInt(10)
            Thread.sleep(ms)
        }
    }
}
```