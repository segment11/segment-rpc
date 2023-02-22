package org.segment.rpc.client

import groovy.transform.CompileStatic
import io.netty.channel.Channel
import org.segment.rpc.server.codec.RpcMessage
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
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
        items[remoteUrl]?.getExclude(excludeOne)
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

    synchronized boolean isLeftActive(RemoteUrl remoteUrl, Channel excludeOne) {
        def channels = items[remoteUrl]
        if (!channels) {
            return false
        }
        channels.isLeftActive(excludeOne)
    }

    synchronized void disconnect(RemoteUrl remoteUrl) {
        items.remove(remoteUrl)?.close()
    }

    synchronized void disconnectAll() {
        items.each { k, v ->
            v.close()
        }
        items.clear()
    }

    synchronized void broadcast(RemoteUrl remoteUrl, RpcMessage msg) {
        def channels = items[remoteUrl]
        if (!channels) {
            return
        }
        channels.broadcast(msg)
    }
}
