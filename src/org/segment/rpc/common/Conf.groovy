package org.segment.rpc.common

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class Conf {

    final static String CONF_FILE_NAME = '/conf.properties'
    Map<String, String> params = [:]

    Conf load() {
        Properties props = [:]
        def stream = Conf.class.getResourceAsStream(CONF_FILE_NAME)
        if (stream) {
            def r = new InputStreamReader(stream, 'utf-8')
            props.load(r)
            r.close()
            props.each { k, v ->
                params[k.toString()] = v.toString()
            }
        }
        this
    }

    Conf loadArgs(String[] args) {
        if (!args) {
            return this
        }

        for (arg in args) {
            def arr = arg.split('=')
            if (arr.size() == 2) {
                params[arr[0]] = arr[1]
            }
        }
        this
    }

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

    Conf put(String key, Object value) {
        params[key] = value.toString()
        this
    }

    Conf on(String key) {
        put(key, 1)
    }

    Conf off(String key) {
        put(key, 0)
    }

    @Override
    String toString() {
        params.toString()
    }
}
