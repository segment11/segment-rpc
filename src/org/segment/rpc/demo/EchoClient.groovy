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
