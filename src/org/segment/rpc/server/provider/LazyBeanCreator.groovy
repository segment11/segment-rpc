package org.segment.rpc.server.provider

import groovy.transform.CompileStatic

@CompileStatic
interface LazyBeanCreator extends BeanCreator {
    Object createLazy()
}