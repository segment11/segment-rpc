package org.segment.rpc.common

import groovy.transform.CompileStatic

@CompileStatic
class Utils {

    static String localIp() {
        InetAddress.localHost.hostAddress
    }

    static String getStackTraceString(Throwable t) {
        if (!t) {
            return ''
        }

        def writer = new StringWriter()
        def pw = new PrintWriter(writer)
        t.printStackTrace(pw)
        pw.flush()
        writer.flush()
        pw.close()
        writer.close()
        writer.toString()
    }
}
