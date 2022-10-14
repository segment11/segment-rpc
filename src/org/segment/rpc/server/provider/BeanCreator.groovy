package org.segment.rpc.server.provider

import groovy.transform.CompileStatic

@CompileStatic
interface BeanCreator {
    Object create()
}