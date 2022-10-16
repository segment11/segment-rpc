package org.segment.rpc.manage

import groovy.transform.CompileStatic

@CompileStatic
class ClientChannelInfo {
    Date registryTime = new Date()

    String address

    ClientChannelInfo() {

    }
}
