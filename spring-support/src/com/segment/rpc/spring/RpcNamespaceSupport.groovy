package com.segment.rpc.spring

import groovy.transform.CompileStatic
import org.springframework.beans.factory.xml.NamespaceHandlerSupport

@CompileStatic
class RpcNamespaceSupport extends NamespaceHandlerSupport {
    @Override
    void init() {
        registerBeanDefinitionParser("segment-rpc-provider", new RpcProviderScanParser())
        registerBeanDefinitionParser("segment-rpc-caller", new RpcCallerBeanParser())
    }

}
