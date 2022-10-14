package org.segment.rpc.invoke

import groovy.transform.CompileStatic

@CompileStatic
class MethodMeta {
    // interface class name
    public String clazz
    public String method
    public Class<?>[] paramTypes = []
    public Object[] args

    @Override
    String toString() {
        "${clazz}.${method} - ${paramTypes?.collect { Class clazz -> clazz.name }}"
    }

    @Override
    boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MethodMeta)) {
            return false
        }
        this.toString() == obj.toString()
    }
}
