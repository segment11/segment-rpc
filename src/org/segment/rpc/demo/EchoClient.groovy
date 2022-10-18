package org.segment.rpc.demo

import org.segment.rpc.client.RpcClient
import org.segment.rpc.common.ConsoleReader
import org.segment.rpc.invoke.ProxyCreator
import org.segment.rpc.server.handler.Req

import java.util.concurrent.TimeoutException

def client = new RpcClient()

// wait and channel will be active
Thread.sleep(2000)

def reader = ConsoleReader.instance
reader.quitHandler = {
    client.stop()
}
reader.lineHandler = { line ->
    println client.sendSync(new Req('/rpc/v1/echo', line))?.body
}
reader.read()

// test method invoke
SayInterface say = new ProxyCreator(client, '/rpc').create(SayInterface)
SayInterface say2 = new ProxyCreator(client, '/rpc').create(SayInterface)
println say == say2
println say.hi('kerry')
println say2.hi('kerry')

def body = "hi kerry".toString()
def resp = client.sendSync(new Req('/rpc/v1/echo', body))
println '' + resp.status + ':' + resp?.body

// not found
def respEmpty = client.sendSync(new Req('/rpc/v2/echo', body))
println '' + respEmpty.status + ':' + respEmpty?.body

// exception
def respEx = client.sendSync(new Req('/rpc/v1/ex', body))
println '' + respEx.status + ':' + respEx?.body

// timeout
try {
    def respTimeout = client.sendSync(new Req('/rpc/v1/timeout', body))
    println '' + respTimeout.status + ':' + respTimeout?.body
} catch (TimeoutException e) {
    println 'timeout and ignore'
}

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
