package org.segment.rpc.demo

import org.segment.rpc.client.RpcClient
import org.segment.rpc.common.ConsoleReader
import org.segment.rpc.invoke.ProxyCreator
import org.segment.rpc.server.handler.Req
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

def log = LoggerFactory.getLogger(this.getClass())

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
def resp = client.sendSync(new Req('/rpc/v1/echo', body).lz4())
println '' + resp.status + ':' + resp?.body

// not found
def respEmpty = client.sendSync(new Req('/rpc/v2/echo', body).gzip())
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

int threadNumber = 100
int loopTimes = 100

AtomicInteger count = new AtomicInteger(0)
AtomicInteger errorCount = new AtomicInteger(0)
def latch = new CountDownLatch(threadNumber)

Set<String> uuidSet = new ConcurrentSkipListSet<>()

threadNumber.times { i ->
    Thread.start {
        try {
            loopTimes.times { j ->
                def req = new Req('/rpc/v1/echo', "hi ${i}, ${j}".toString())
                uuidSet << req.uuid
                try {
                    def resp2 = client.sendSync(req)
                    uuidSet.remove(resp2.uuid)
                    if (resp2.ok()) {
                        println '' + resp.status + ':' + resp2.body
                    } else {
                        println '' + resp.status + ':' + resp2.message
                    }
//                  println say.hi("kerry ${i}, ${j}".toString())
                    count.incrementAndGet()
                    // mock do business
                    long ms = 10 + new Random().nextInt(100)
                    Thread.sleep(ms)
                } catch (Exception e) {
                    if (e instanceof TimeoutException) {
                        log.warn('client send timeout, uuid: ' + req.uuid)
                    } else {
                        log.error('client send error, uuid: ' + req.uuid, e)
                    }
                    errorCount.incrementAndGet()
                }
            }
        } finally {
            latch.countDown()
        }
    }
}

latch.await()
println 'all requests send'

println 'ok count - ' + count.get()
println 'error count - ' + errorCount.get()
println 'left uuid - ' + uuidSet
