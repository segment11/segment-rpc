package org.segment.rpc.stats

import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@CompileStatic
@Singleton
class CounterInMinute {
    // save last minute number, like 404/505 count
    private Map<String, AtomicLong> cache = new ConcurrentHashMap<>()

    private String currentMinute(StatsType... types) {
        int minute = (System.currentTimeMillis() / 1000 / 60).intValue()
        String[] r = new String[types.length + 1]
        r[0] = minute.toString()
        for (int i = 0; i < types.length; i++) {
            r[i + 1] = types[i].name
        }
        r.join(',')
    }

    long increaseAndGet(long add, StatsType... types) {
        String key = currentMinute(types)
        AtomicLong value = new AtomicLong(add)
        def old = cache.putIfAbsent(key, value)
        if (old != null) {
            return old.addAndGet(add)
        } else {
            return add
        }
    }

    long get(StatsType... types) {
        def old = cache.get(currentMinute(types))
        old == null ? 0 : old.get()
    }

}
