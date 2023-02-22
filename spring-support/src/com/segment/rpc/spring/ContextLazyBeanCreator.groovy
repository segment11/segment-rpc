package com.segment.rpc.spring

import groovy.transform.CompileStatic
import org.segment.rpc.server.provider.LazyBeanCreator
import org.springframework.context.ApplicationContext

@CompileStatic
class ContextLazyBeanCreator implements LazyBeanCreator {
    ApplicationContext context
    Class clazz

    ContextLazyBeanCreator(ApplicationContext context, Class clazz) {
        this.context = context
        this.clazz = clazz
    }

    @Override
    Object create() {
        null
    }

    @Override
    Object createLazy() {
        def beans = context.getBeansOfType(clazz)
        if (!beans) {
            return null
        }
        // exclude Remote for client
        def entry = beans.find { !it.key.endsWith(RpcCallerBeanParser.BEAN_NAME_SUFFIX) }
        entry?.value
    }
}
