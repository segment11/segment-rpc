package org.segment.rpc.common

import groovy.transform.CompileStatic

@CompileStatic
abstract class AbstractConf {
    Map<String, String> params = [:]

    String get(String key) {
        params[key]
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
