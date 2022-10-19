package org.segment.rpc.common

import groovy.transform.CompileStatic
import org.segment.rpc.client.loadbalance.RandomLoadBalance
import org.segment.rpc.client.loadbalance.LoadBalance
import org.segment.rpc.server.registry.Registry
import org.segment.rpc.server.registry.local.LocalRegistry
import org.segment.rpc.server.registry.zookeeper.ZookeeperRegistry

@CompileStatic
class SpiSupport {
    static ClassLoader cl = SpiSupport.class.classLoader

    static Registry getRegistry(RpcConf c) {
        if (c.isOn('registry.use.local')) {
            return LocalRegistry.instance
        }

        ServiceLoader.load(Registry, cl).find { it.class.name.startsWith('vendor') } as Registry
                ?: new ZookeeperRegistry()
    }

    static LoadBalance getLoadBalance() {
        ServiceLoader.load(LoadBalance, cl).find { it.class.name.startsWith('vendor') } as LoadBalance
                ?: RandomLoadBalance.instance
    }

}
