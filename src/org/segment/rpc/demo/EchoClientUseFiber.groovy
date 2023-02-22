package org.segment.rpc.demo

import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.fibers.SuspendExecution
import co.paralleluniverse.strands.SuspendableRunnable
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.client.RpcClient
import org.segment.rpc.common.ConsoleReader
import org.segment.rpc.invoke.ProxyCreator
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp
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

int fiberNumber = 10000

AtomicInteger okCount = new AtomicInteger(0)
AtomicInteger rejectCount = new AtomicInteger(0)
AtomicInteger errorCount = new AtomicInteger(0)
def latch = new CountDownLatch(fiberNumber)

Set<String> uuidSet = new ConcurrentSkipListSet<>()

def beginT = System.currentTimeMillis()

@CompileStatic
@Slf4j
class Inner implements SuspendableRunnable {
    AtomicInteger okCount
    AtomicInteger rejectCount
    AtomicInteger errorCount
    CountDownLatch latch

    Set<String> uuidSet

    RpcClient client

    int i

    @Override
    void run() throws SuspendExecution, InterruptedException {
        try {
            def req = new Req('/rpc/v1/echo', "hi ${i}".toString())
            uuidSet << req.uuid
            try {
                def resp2 = client.sendSync(req)
                uuidSet.remove(resp2.uuid)
                println '' + resp2.status + ':' + resp2.body
                if (resp2.ok()) {
                    okCount.incrementAndGet()
                } else {
                    if (resp2.status == Resp.Status.REJECT) {
                        rejectCount.incrementAndGet()
                    } else {
                        errorCount.incrementAndGet()
                    }
                }

                // mock do business
                Fiber.sleep(20)
            } catch (Exception e) {
                if (e instanceof TimeoutException) {
                    log.warn('client send timeout, uuid: ' + req.uuid)
                } else {
                    log.error('client send error, uuid: ' + req.uuid, e)
                }
                errorCount.incrementAndGet()
            }
        } finally {
            latch.countDown()
        }
    }
}

fiberNumber.times { i ->
    new Fiber<Void>('one fiber ' + i,
            new Inner(i: i, okCount: okCount, rejectCount: rejectCount, errorCount: errorCount,
                    latch: latch, uuidSet: uuidSet, client: client)).start()
}

latch.await()
println 'all requests send'

def costT = System.currentTimeMillis() - beginT
println 'cost time - ' + costT + 'ms'
println 'ok count - ' + okCount.get()
println 'reject count - ' + rejectCount.get()
println 'error count - ' + errorCount.get()
println 'left uuid - ' + uuidSet
