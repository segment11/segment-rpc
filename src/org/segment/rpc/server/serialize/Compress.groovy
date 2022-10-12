package org.segment.rpc.server.serialize

import groovy.transform.CompileStatic

@CompileStatic
interface Compress {
    byte[] compress(byte[] data)

    byte[] decompress(byte[] data)
}