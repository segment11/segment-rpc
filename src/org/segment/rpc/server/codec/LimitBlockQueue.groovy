package org.segment.rpc.server.codec

import groovy.transform.CompileStatic

import java.util.concurrent.LinkedBlockingQueue

@CompileStatic
class LimitBlockQueue<E> extends LinkedBlockingQueue<E> {
    private int limit

    LimitBlockQueue(int limit) {
        this.limit = limit
    }

    int getLimit() {
        return limit
    }

    @Override
    boolean offer(E e) {
        if (size() >= limit) {
            poll()
        }
        super.offer(e)
    }
}
