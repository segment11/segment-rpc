package org.segment.rpc.common

import groovy.transform.CompileStatic

@CompileStatic
class RpcConf {

    final static String ZK_CONNECT_STRING = 'zookeeper.connect.string'
    final static String ZK_PATH_PREFIX = 'zookeeper.path.prefix'

    final static String CONF_FILE_NAME = '/conf.properties'

    Map<String, String> params = [:]

    RpcConf extend(Map<String, Object> params = null) {
        if (params) {
            for (entry in params.entrySet()) {
                this.params[entry.key] = entry.value.toString()
            }
        }
        this
    }

    private static volatile RpcConf localOne

    static RpcConf fromLoad() {
        if (localOne) {
            return localOne
        }
        def c = new RpcConf()
        c.load()
        localOne = c
        c
    }

    private RpcConf load() {
        Properties props = [:]
        def stream = RpcConf.class.getResourceAsStream(CONF_FILE_NAME)
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

    RpcConf loadArgs(String[] args) {
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

    RpcConf put(String key, Object value) {
        params[key] = value.toString()
        this
    }

    RpcConf on(String key) {
        put(key, 1)
    }

    RpcConf off(String key) {
        put(key, 0)
    }

    @Override
    String toString() {
        params.toString()
    }
}
