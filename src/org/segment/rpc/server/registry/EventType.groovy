package org.segment.rpc.server.registry

import groovy.transform.CompileStatic

@CompileStatic
enum EventType {
    NEW_ADDED, OLD_REMOVED, ACTIVE, INACTIVE
}
