package org.segment.rpc.server.registry

import groovy.transform.CompileStatic

@CompileStatic
class RemoteUrl {
    Protocol protocol = Protocol.SEGMENT_RPC

    String host

    int port

    String context

    InetSocketAddress address() {
        new InetSocketAddress(host, port)
    }

    static enum Protocol {
        SEGMENT_RPC
    }

    @Override
    boolean equals(Object obj) {
        if (obj == null || !(obj instanceof RemoteUrl)) {
            return false
        }
        def other = obj as RemoteUrl
        this.host == other.host && this.port == other.port
    }

    @Override
    int hashCode() {
        this.toString().hashCode()
    }

    @Override
    String toString() {
        "${address().toString()}"
    }
}
