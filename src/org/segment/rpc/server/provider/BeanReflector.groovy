package org.segment.rpc.server.provider

import com.esotericsoftware.reflectasm.MethodAccess
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Slf4j
class BeanReflector {

    private MethodAccess ma

    private int maIndex = -1

    private BeanReflector(MethodAccess ma, int maIndex) {
        this.ma = ma
        this.maIndex = maIndex
    }

    Class getReturnType() {
        Class[] rt = ma.returnTypes
        rt.length > maIndex ? rt[maIndex] : null
    }

    Object invoke(Object bean, Object... args) {
        if (!ma || maIndex == -1) {
            return null
        }
        ma.invoke(bean, maIndex, args)
    }

    private static ConcurrentHashMap<String, MethodAccess> cachedMa = new ConcurrentHashMap<>()
    private static ConcurrentHashMap<String, Integer> cachedMaIndex = new ConcurrentHashMap<>()

    static MethodAccess getMaCached(Class clz) {
        String key = clz.name
        def ma = cachedMa.get(key)
        if (ma != null) {
            return ma
        }

        log.info 'get method access for class: {}', clz.name
        def ma2 = MethodAccess.get(clz)
        def old = cachedMa.putIfAbsent(key, ma2)
        if (old) {
            return old
        } else {
            return ma2
        }
    }

    static BeanReflector get(Class clz, String methodName, Class... paramTypes) {
        String key = clz.getName() + '.' + methodName + '(' + (paramTypes ? paramTypes.collect { ((Class) it).name }.join(',') : '') + ')'
        def maIndexCached = cachedMaIndex.get(key)
        if (maIndexCached != null && maIndexCached.intValue() == -1) {
            return null
        }

        def ma = getMaCached(clz)
        if (maIndexCached != null && maIndexCached.intValue() != -1) {
            return new BeanReflector(ma, maIndexCached)
        }

        log.info 'get method access index for method: {}', key
        def maIndex = paramTypes ? ma.getIndex(methodName, paramTypes) : ma.getIndex(methodName)
        cachedMaIndex.putIfAbsent(key, maIndex)
        return new BeanReflector(ma, maIndex)
    }

}
