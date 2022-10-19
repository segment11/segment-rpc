package org.segment.rpc.client

import groovy.transform.CompileStatic

@CompileStatic
class RemoteUrlDownException extends RuntimeException {
    RemoteUrlDownException(String s) {
        super(s)
    }
}
