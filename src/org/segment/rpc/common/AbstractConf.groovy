package org.segment.rpc.common

import groovy.transform.CompileStatic

@CompileStatic
abstract class AbstractConf {
    Map<String, Object> params = [:]

    AbstractConf extend(Map<String, Object> params = null, boolean isOverWrite = true) {
        if (params) {
            for (entry in params.entrySet()) {
                def old = this.params[entry.key]
                if (old == null || isOverWrite) {
                    this.params[entry.key] = entry.value.toString()
                }
            }
        }
        this
    }

    String get(String key) {
        params[key] as String
    }

    String getString(String key, String defaultValue) {
        get(key) ?: defaultValue
    }

    int getInt(String key, int defaultValue) {
        def s = get(key)
        s ? s as int : defaultValue
    }

    boolean isOn(String key) {
        '1' == get(key)
    }

    AbstractConf put(String key, Object value) {
        params[key] = value.toString()
        this
    }

    AbstractConf on(String key) {
        put(key, 1)
    }

    AbstractConf off(String key) {
        put(key, 0)
    }

    @Override
    String toString() {
        params.toString()
    }
}
