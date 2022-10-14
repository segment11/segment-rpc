package org.segment.rpc.server.codec

import groovy.transform.CompileStatic

@CompileStatic
class Stats {
    private LimitBlockQueue<Item> queue = new LimitBlockQueue<>(10000)

    void add(long number) {
        queue.offer(new Item(number))
    }

    long getLatestMin() {
        long current = System.currentTimeMillis()
        long passed = current - 1000 * 60

        long total = 0

        def it = queue.iterator()
        while (it.hasNext()) {
            def item = it.next()
            def millis = item.millis
            if (millis > passed) {
                total += item.number
            }
        }

        total
    }

    static class Item {
        long millis
        long number = 0

        Item(long number) {
            this.number = number
            this.millis = System.currentTimeMillis()
        }
    }
}
