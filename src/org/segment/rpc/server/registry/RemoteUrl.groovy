package org.segment.rpc.server.registry

import groovy.transform.CompileStatic

@CompileStatic
class RemoteUrl {
    static final int DEFAULT_WEIGHT = 10

    Protocol protocol = Protocol.SEGMENT_RPC

    private String host

    private int port

    String context = '/rpc'

    boolean ready = true

    int weight = DEFAULT_WEIGHT

    Date updatedTime = new Date()

    String getHost() {
        return host
    }

    int getPort() {
        return port
    }

    RemoteUrl(String host, int port) {
        this.host = host
        this.port = port
    }

    InetSocketAddress address() {
        new InetSocketAddress(host, port)
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
        "${host}:${port}"
    }

    String toStringView() {
        "${context}/${host}:${port},ready=${ready},weight=${weight},updatedTime:${updatedTime}".toString()
    }

    static enum Protocol {
        SEGMENT_RPC
    }
}
