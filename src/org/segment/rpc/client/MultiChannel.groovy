package org.segment.rpc.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.channel.Channel
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
@Slf4j
class MultiChannel {
    private AtomicInteger index = new AtomicInteger(0)

    private LinkedList<Channel> channels = new LinkedList<>()

    private RemoteUrl remoteUrl

    MultiChannel(RemoteUrl remoteUrl) {
        this.remoteUrl = remoteUrl
    }

    // get one active channel round robin
    Channel get() {
        if (!channels) {
            return null
        }

        def activeChannels = channels.findAll { it.isActive() }
        if (!activeChannels) {
            return null
        }

        def i = index.incrementAndGet()
        return activeChannels[i % activeChannels.size()]
    }

    synchronized void add(Channel channel) {
        channels.add(channel)
    }

    synchronized void remove(Channel channel) {
        channels.remove(channel)
    }

    void close() {
        channels.each { v ->
            if (!v.isOpen()) {
                return
            }
            try {
                log.info 'ready to disconnect {}', remoteUrl
                v.close()
                log.info 'done disconnect {}', remoteUrl
            } catch (Exception e) {
                log.error('disconnect channel error - ' + remoteUrl, e)
            }
        }
    }

    boolean isLeftActive(Channel channel) {
        for (int i = 0; i < channels.size(); i++) {
            def c = channels[i]
            if (c != channel && c.isActive()) {
                return true
            }
        }
        false
    }
}
