package org.segment.rpc.server.registry

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class EventHandler {
    // happen every little
    List<EventTrigger> eventList = Collections.synchronizedList(new LinkedList<EventTrigger>())

    void add(EventTrigger trigger) {
        eventList.add(trigger)
    }

    void fire(RemoteUrl remoteUrl, EventType type) {
        eventList.each {
            if (it.type() == type) {
                try {
                    it.handle(remoteUrl)
                } catch (Exception e) {
                    log.error 'event fire handle error - ' + remoteUrl + ' - type - ' + type.toString(), e
                }
            }
        }
    }
}
