package org.segment.rpc.common

import com.google.common.util.concurrent.ThreadFactoryBuilder
import groovy.transform.CompileStatic

import java.util.concurrent.ThreadFactory

@CompileStatic
class Utils {
    static boolean isWindows() {
        System.getProperty('os.name').toLowerCase().contains('windows')
    }

    static String projectPath(String relativePath = '') {
        def workDir = new File('.').absolutePath.replaceAll("\\\\", '/').
                replace('/src/', '')
        if (workDir[-1] == '.') {
            return workDir[0..-2] + relativePath
        }
        workDir + relativePath
    }

    static String localIp() {
        InetAddress.localHost.hostAddress
    }

    static void stopWhenConsoleQuit(Closure<Void> closure, InputStream is = null) {
        boolean isStopped = false
        Runtime.addShutdownHook {
            if (!isStopped) {
                closure.call()
            }
        }

        if (isWindows()) {
            Thread.start {
                def br = new BufferedReader(new InputStreamReader(is ?: System.in))
                while (true) {
                    if (br.readLine() == 'quit') {
                        println 'quit from console...'
                        closure.call()
                        isStopped = true
                        break
                    }
                }
            }
        }
    }

    static ThreadFactory createThreadFactory(String namePrefix, boolean daemon) {
        new ThreadFactoryBuilder()
                .setNameFormat(namePrefix + "-%d")
                .setDaemon(daemon).build()
    }
}
