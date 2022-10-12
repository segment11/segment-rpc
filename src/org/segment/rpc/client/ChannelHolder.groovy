package org.segment.rpc.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.channel.Channel
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Singleton
@Slf4j
class ChannelHolder {
    private final Map<RemoteUrl, Channel> items = new ConcurrentHashMap<>()

    Channel get(RemoteUrl remoteUrl) {
        def r = items[remoteUrl]
        if (r && !r.isActive()) {
            log.warn 'channel not active {}', r.remoteAddress()
            items.remove(remoteUrl)
        }
        r
    }

    void put(RemoteUrl remoteUrl, Channel channel) {
        items.put(remoteUrl, channel)
    }

    void disconnect() {
        items.each { k, v ->
            log.info 'ready to disconnect {}', k.toString()
            v.close()
            log.info 'done disconnect {}', k.toString()
        }
    }
}
