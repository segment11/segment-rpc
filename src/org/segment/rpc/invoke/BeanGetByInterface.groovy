package org.segment.rpc.invoke

import groovy.transform.CompileStatic

@CompileStatic
interface BeanGetByInterface {
    Object get(String interfaceName)
}
