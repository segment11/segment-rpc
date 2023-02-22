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

    List<MethodMeta> allMethods() {
        List<MethodMeta> r = []
        map.each { k, v ->
            for (it in v) {
                r << it.meta
            }
        }
        r
    }

    @Override
    void provide(Class interfaceClass, Object target) {
        if (!target.class.interfaces.any { it == interfaceClass } && !(target instanceof BeanCreator)) {
            throw new IllegalArgumentException("target class ${target.class.name} does not implement interface ${interfaceClass.name}")
        }

        def list = new CopyOnWriteArrayList<MethodWrapper>()
        def oldList = map.putIfAbsent(interfaceClass.name, list)
        def targetList = oldList ?: list

        def finalTarget = target instanceof BeanCreator ? ((BeanCreator) target).create() : target

        for (method in interfaceClass.getMethods()) {
            // lazy create if final target is null
            if (finalTarget) {
                // prepare fast method access cache
                BeanReflector.get(finalTarget.class, method.name, method.parameterTypes)
            }

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
        if (target instanceof LazyBeanCreator) {
            wrapper.target = ((LazyBeanCreator) target).createLazy()
        } else if (target instanceof BeanCreator) {
            wrapper.target = ((BeanCreator) target).create()
        } else {
            wrapper.target = target
        }
        wrapper
    }
}
