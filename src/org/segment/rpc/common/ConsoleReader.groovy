package org.segment.rpc.common

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class ConsoleReader {
    Closure lineHandler

    Closure quitHandler

    Inner inner = new Inner()

    void read() {
        inner.name = 'loop read from console'
        inner.reader = this
        inner.start()
    }

    void stop() {
        println 'quit'
        inner.flag = false
    }

    @CompileStatic
    class Inner extends Thread {
        ConsoleReader reader

        volatile boolean flag = true

        @Override
        void run() {
            def br = new BufferedReader(new InputStreamReader(System.in))
            while (flag) {
                // never end unless give line
                def line = br.readLine()
                if (line == null) {
                    continue
                }

                if ('quit' == line && reader.quitHandler) {
                    reader.quitHandler.call()
                    flag = false
                    break
                }

                if (reader.lineHandler != null) {
                    try {
                        reader.lineHandler.call(line)
                    } catch (Exception e) {
                        e.printStackTrace()
                    }
                } else {
                    println '...'
                }
            }
        }
    }
}
