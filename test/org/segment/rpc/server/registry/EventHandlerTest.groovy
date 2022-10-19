package org.segment.rpc.server.registry

import spock.lang.Specification

class EventHandlerTest extends Specification {
    def testFire() {
        given:
        def handler = new EventHandler()
        int count = 0
        and:
        handler.add(new EventTrigger() {
            @Override
            EventType type() {
                EventType.NEW_ADDED
            }

            @Override
            void handle(RemoteUrl remoteUrl) {
                println 'fire - ' + remoteUrl
                count++
            }
        })
        handler.fire(new RemoteUrl('', 0), EventType.NEW_ADDED)
        handler.fire(new RemoteUrl('', 0), EventType.NEW_ADDED)
        expect:
        count == 2
    }
}
