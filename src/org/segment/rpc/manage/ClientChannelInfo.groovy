package org.segment.rpc.manage

import groovy.transform.CompileStatic

@CompileStatic
class ClientChannelInfo implements Comparable<ClientChannelInfo> {
    Date registeredTime = new Date()

    String address

    ClientChannelInfo(String address) {
        this.address = address
    }

    @Override
    int compareTo(ClientChannelInfo o) {
        this.registeredTime.time < o.registeredTime.time ? -1 : 1
    }
}
