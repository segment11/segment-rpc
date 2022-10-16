package org.segment.rpc.server.registry

import groovy.transform.CompileStatic

@CompileStatic
abstract class EventTrigger {
    abstract EventType type()

    abstract handle(RemoteUrl remoteUrl)
}
