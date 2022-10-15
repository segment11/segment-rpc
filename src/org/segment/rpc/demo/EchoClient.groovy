package org.segment.rpc.demo

import org.segment.rpc.client.RpcClient
import org.segment.rpc.common.ConsoleReader
import org.segment.rpc.invoke.ProxyCreator
import org.segment.rpc.server.handler.Req

import java.util.concurrent.CountDownLatch

def client = new RpcClient()

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

def resp = client.sendSync(new Req('/rpc/v1/echo', "hi kerry".toString()))
println '' + resp.status + ':' + resp?.body

// not found
def respEmpty = client.sendSync(new Req('/rpc/v2/echo', "hi kerry".toString()))
println '' + respEmpty.status + ':' + respEmpty?.body

int threadNumber = 10
int loopTimes = 10

def latch = new CountDownLatch(threadNumber)

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
        latch.countDown()
    }
}

latch.await()
println 'requests sent done'

