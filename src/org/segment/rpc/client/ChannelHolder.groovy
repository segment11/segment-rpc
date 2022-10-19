package org.segment.rpc.client

import groovy.transform.CompileStatic
import io.netty.channel.Channel
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Singleton
class ChannelHolder {
    private final Map<RemoteUrl, MultiChannel> items = new ConcurrentHashMap<>()

    Channel getActive(RemoteUrl remoteUrl) {
        def channels = items[remoteUrl]
        if (!channels) {
            return null
        }
        channels.get()
    }

    Channel getActiveExcludeOne(RemoteUrl remoteUrl, Channel excludeOne) {
        def channels = items[remoteUrl]
        if (!channels) {
            return null
        }
        channels.getExclude(excludeOne)
    }

    synchronized int getActiveNumber(RemoteUrl remoteUrl) {
        def channels = items[remoteUrl]
        if (!channels) {
            return 0
        }
        channels.getActiveNumber()
    }

    synchronized void add(RemoteUrl remoteUrl, Channel channel) {
        def channels = new MultiChannel(remoteUrl)
        channels.add(channel)
        def old = items.putIfAbsent(remoteUrl, channels)
        if (old) {
            old.add(channel)
        }
    }

    synchronized void remove(RemoteUrl remoteUrl) {
        items.remove(remoteUrl)
    }

    synchronized void remove(RemoteUrl remoteUrl, Channel channel) {
        def channels = items[remoteUrl]
        if (!channels) {
            return
        }

        channels.remove(channel)
    }

    synchronized boolean isLeftActive(RemoteUrl remoteUrl, Channel channel) {
        def channels = items[remoteUrl]
        if (!channels) {
            return false
        }
        channels.isLeftActive(channel)
    }

    synchronized void disconnect() {
        items.each { k, v ->
            v.close()
        }
        items.clear()
    }
}
