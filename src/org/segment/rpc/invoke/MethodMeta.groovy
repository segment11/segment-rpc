package org.segment.rpc.invoke

import groovy.transform.CompileStatic

@CompileStatic
class MethodMeta {
    public String clazz
    public String method
    public Class<?>[] paramTypes
    public Object[] args

    @Override
    String toString() {
        "${clazz}.${method} - [${paramTypes.collect { Class clazz -> clazz.name }}]"
    }
}
