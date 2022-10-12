package org.segment.rpc.common

import groovy.transform.CompileStatic
import org.segment.rpc.client.DefaultLoadBalance
import org.segment.rpc.client.LoadBalance
import org.segment.rpc.server.registry.Registry
import org.segment.rpc.server.registry.zookeeper.ZookeeperRegistry

@CompileStatic
class SpiSupport {
    static ClassLoader cl = SpiSupport.class.classLoader

    static Registry getRegistry() {
        ServiceLoader.load(Registry, cl).find { it.class.name.startsWith('vendor') } as Registry
                ?: ZookeeperRegistry.instance
    }

    static LoadBalance getLoadBalance() {
        ServiceLoader.load(LoadBalance, cl).find { it.class.name.startsWith('vendor') } as LoadBalance
                ?: DefaultLoadBalance.instance
    }

}
