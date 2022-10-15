package org.segment.rpc.common

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class ConsoleReader {
    Closure lineHandler

    Closure quitHandler

    volatile boolean flag = true

    void read() {
        Thread.start {
            def br = new BufferedReader(new InputStreamReader(System.in))
            while (flag) {
                def line = br.readLine()
                if (line == null) {
                    continue
                }

                if ('quit' == line && quitHandler) {
                    quitHandler.call()
                    flag = false
                    break
                }

                if (lineHandler != null) {
                    lineHandler.call(line)
                } else {
                    println '...'
                }
            }
        }
    }

    void stop() {
        flag = false
    }
}
