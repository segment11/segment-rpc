package org.segment.rpc.common

def reader = ConsoleReader.instance
reader.quitHandler = {
    reader.stop()
}
reader.read()

Thread.sleep(2000)
reader.stop()
// readLine still runnable

//System.exit(0)
