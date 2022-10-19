package org.segment.rpc.server.registry

import groovy.transform.CompileStatic

@CompileStatic
enum EventType {
    NEW_ADDED, // registry found new remote server and add to local
    OLD_REMOVED, // registry found a remote server disconnect and remove it from local if exists
    ACTIVE, // channel active
    INACTIVE // channel inactive
}
