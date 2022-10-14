package org.segment.rpc.server.provider

import groovy.transform.CompileStatic
import org.segment.rpc.invoke.MethodMeta

import java.lang.reflect.Method

@CompileStatic
class MethodWrapper {
    Method method
    MethodMeta meta
    Object target
}
