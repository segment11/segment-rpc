package org.segment.rpc.client

import groovy.transform.CompileStatic
import io.netty.channel.Channel
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Singleton
class ChannelHolder {
    private final Map<RemoteUrl, MultiChannel> items = new ConcurrentHashMap<>()

    Channel get(RemoteUrl remoteUrl) {
        def channels = items[remoteUrl]
        if (!channels) {
            return null
        }
        channels.get()
    }

    void add(RemoteUrl remoteUrl, Channel channel) {
        def channels = new MultiChannel(remoteUrl)
        channels.add(channel)
        def old = items.putIfAbsent(remoteUrl, channels)
        if (old) {
            old.add(channel)
        }
    }

    boolean isLeftActive(RemoteUrl remoteUrl, Channel channel) {
        def channels = items[remoteUrl]
        if (!channels) {
            return false
        }
        channels.isLeftActive(channel)
    }

    void disconnect() {
        items.each { k, v ->
            v.close()
        }
        items.clear()
    }
}
