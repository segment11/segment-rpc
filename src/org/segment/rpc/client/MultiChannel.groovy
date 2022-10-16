package org.segment.rpc.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.channel.Channel
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

@CompileStatic
@Slf4j
class MultiChannel {
    private AtomicInteger index = new AtomicInteger(0)

    private LinkedList<Channel> channels = new LinkedList<>()

    private ReadWriteLock lock = new ReentrantReadWriteLock()

    private RemoteUrl remoteUrl

    MultiChannel(RemoteUrl remoteUrl) {
        this.remoteUrl = remoteUrl
    }

    Channel get() {
        if (!channels) {
            return null
        }

        lock.readLock().lock()
        try {
            def activeChannels = channels.findAll { it.isActive() }
            if (!activeChannels) {
                return null
            }

            def i = index.incrementAndGet()
            return activeChannels[i % activeChannels.size()]
        } finally {
            lock.readLock().unlock()
        }
    }

    void add(Channel channel) {
        lock.writeLock().lock()
        try {
            channels.add(channel)
        } finally {
            lock.writeLock().lock()
        }
    }

    void remove(Channel channel) {
        lock.writeLock().lock()
        try {
            channels.remove(channel)
        } finally {
            lock.writeLock().lock()
        }
    }

    void close() {
        lock.writeLock().lock()
        try {
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
        } finally {
            lock.writeLock().lock()
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
