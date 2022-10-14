# segment-rpc
A Java simple RPC framework based on netty writen in Groovy. Use routers instead of method invoke.

## features
> 1. registry support local and zookeeper
> 2. load balance
> 3. metrics

## todos
> 1. dashboard / management

## Echo Client-Server Demo


### change conf.properties
zookeeper.connect.string=192.168.99.100:2181

### EchoServer.groovy
```groovy
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
```

### EchoClient.groovy
```groovy
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
// simple call
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

```