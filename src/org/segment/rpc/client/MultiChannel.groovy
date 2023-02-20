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

    // get one active channel round robin
    Channel getExclude(Channel excludeOne) {
        channels.find { it.isActive() && it != excludeOne }
    }

    int getActiveNumber() {
        int number = 0
        for (it in channels) {
            if (it.isActive()) {
                number++
            }
        }
        number
    }

    synchronized void add(Channel channel) {
        channels.add(channel)
    }

    synchronized void remove(Channel channel) {
        channels.remove(channel)
    }

    synchronized void close() {
        channels.each {
            if (!it.isOpen()) {
                return
            }

            def address = it.remoteAddress()
            try {
                log.info 'ready to disconnect {}', address
                it.close()
                log.info 'done disconnect {}', address
            } catch (Exception e) {
                log.error('disconnect channel error - ' + address, e)
            }
        }
    }

    boolean isLeftActive(Channel excludeOne) {
        channels.any { it.isActive() && it != excludeOne }
    }
}
