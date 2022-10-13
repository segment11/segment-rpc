package org.segment.rpc.demo

import org.segment.rpc.client.RpcClient
import org.segment.rpc.common.Utils
import org.segment.rpc.server.handler.Req

def client = new RpcClient()

Utils.stopWhenConsoleQuit {
    client.stop()
}

int threadNumber = 10
int loopTimes = 10
threadNumber.times { i ->
    Thread.start {
        loopTimes.times { j ->
            def resp = client.sendSync(new Req('/rpc/v1/echo', "hi ${i}, ${j}".toString()))
            println resp?.body
            // mock do business
            long ms = 10 + new Random().nextInt(10)
            Thread.sleep(ms)
        }
    }
}
