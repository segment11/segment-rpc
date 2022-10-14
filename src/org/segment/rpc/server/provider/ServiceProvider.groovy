package org.segment.rpc.server.provider

import groovy.transform.CompileStatic
import org.segment.rpc.invoke.MethodMeta

@CompileStatic
interface ServiceProvider {
    void provide(Class interfaceClass, Object target)

    MethodWrapper lookupMethod(MethodMeta meta)
}
