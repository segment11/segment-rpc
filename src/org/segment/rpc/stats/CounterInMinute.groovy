package org.segment.rpc.stats

import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@CompileStatic
@Singleton
class CounterInMinute {
    // save last minute number, like 404/505 count
    private Map<String, AtomicLong> cacheInMinute = new ConcurrentHashMap<>()
    private Map<String, Integer> cacheOne = new ConcurrentHashMap<>()

    void setOne(int value, StatsType type) {
        cacheOne[type.name] = value
    }

    int getOne(StatsType type) {
        cacheOne[type.name] ?: 0
    }

    private String addCurrentMinutePrefix(StatsType type) {
        int minute = (System.currentTimeMillis() / 1000 / 60).intValue()
        "${minute},${type.name}"
    }

    long increaseAndGet(long add, StatsType type) {
        String key = addCurrentMinutePrefix(type)
        AtomicLong value = new AtomicLong(add)
        def old = cacheInMinute.putIfAbsent(key, value)
        if (old != null) {
            return old.addAndGet(add)
        } else {
            return add
        }
    }

    long getCounter(StatsType type) {
        def old = cacheInMinute.get(addCurrentMinutePrefix(type))
        old == null ? 0 : old.get()
    }

    // clear passed 2 minute cached stats
    void clearAllOldCounter() {
        int currentMinute = (System.currentTimeMillis() / 1000 / 60 - 2).intValue()
        def it = cacheInMinute.entrySet().iterator()
        while (it.hasNext()) {
            def entry = it.next()
            def key = entry.key
            def minute = key.split(',')[0] as int
            if (minute < currentMinute) {
                it.remove()
            }
        }
    }

}
