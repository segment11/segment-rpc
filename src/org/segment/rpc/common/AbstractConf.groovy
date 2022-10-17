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

    @Override
    String toString() {
        params.toString()
    }
}
