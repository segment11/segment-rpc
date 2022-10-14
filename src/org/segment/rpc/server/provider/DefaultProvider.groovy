package org.segment.rpc.server.provider

import groovy.transform.CompileStatic
import org.segment.rpc.invoke.MethodMeta

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@CompileStatic
@Singleton
class DefaultProvider implements ServiceProvider {

    private Map<String, CopyOnWriteArrayList<MethodWrapper>> map = new ConcurrentHashMap<>()
    private Map<String, Object> beans = new ConcurrentHashMap<>()

    List<MethodMeta> listMethodsByInterface(Class interfaceClass) {
        map.get(interfaceClass.name)?.collect {
            it.meta
        }
    }

    @Override
    void provide(Class interfaceClass, Object target) {
        def list = new CopyOnWriteArrayList<MethodWrapper>()
        def oldList = map.putIfAbsent(interfaceClass.name, list)
        def targetList = oldList ?: list

        for (method in interfaceClass.getMethods()) {
            def meta = new MethodMeta()
            meta.clazz = interfaceClass.name
            meta.method = method.name
            meta.paramTypes = method.parameterTypes
            targetList << new MethodWrapper(meta: meta, method: method)
        }

        beans[interfaceClass.name] = target
    }

    @Override
    MethodWrapper lookupMethod(MethodMeta meta) {
        def list = map[meta.clazz]
        if (!list) {
            return null
        }
        def wrapper = list.find { it.meta == meta }
        if (!wrapper) {
            return null
        }
        def target = beans.get(meta.clazz)
        if (target instanceof BeanCreator) {
            wrapper.target = ((BeanCreator) target).create()
        } else {
            wrapper.target = target
        }
        wrapper
    }
}
