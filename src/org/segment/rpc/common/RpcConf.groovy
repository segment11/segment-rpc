package org.segment.rpc.common

import groovy.transform.CompileStatic

@CompileStatic
class RpcConf extends AbstractConf {

    final static String ZK_CONNECT_STRING = 'zookeeper.connect.string'
    final static String ZK_PATH_PREFIX = 'zookeeper.path.prefix'

    final static String CLIENT_CHANNEL_NUMBER_PER_SERVER = 'client.channel.number.per.server'

    final static String CONF_FILE_NAME = '/conf.properties'

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
}
