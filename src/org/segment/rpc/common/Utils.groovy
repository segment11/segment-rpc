package org.segment.rpc.common


import groovy.transform.CompileStatic

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
