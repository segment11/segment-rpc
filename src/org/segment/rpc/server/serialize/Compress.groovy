package org.segment.rpc.server.serialize

import groovy.transform.CompileStatic

@CompileStatic
interface Compress {
    void compress(InputStream is, OutputStream os)

    void decompress(InputStream is, OutputStream os)
}