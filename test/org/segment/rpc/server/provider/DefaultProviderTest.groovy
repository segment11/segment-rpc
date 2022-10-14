package org.segment.rpc.server.provider

import org.segment.rpc.invoke.MethodMeta
import spock.lang.Specification

class DefaultProviderTest extends Specification {
    static interface A {
        void hi()
    }

    static class AImpl implements A {

        @Override
        void hi() {
            println 'hi'
        }
    }

    void testProvide() {
        given:
        def p = DefaultProvider.instance
        p.provide(A.class, new AImpl())
        def list = p.listMethodsByInterface(A.class)
        list.each {
            println it
        }
        expect:
        list.size() > 0
    }

    void testLookupMethod() {
        given:
        def p = DefaultProvider.instance
        def impl = new AImpl()
        p.provide(A.class, impl)
        def wrapper = p.lookupMethod(new MethodMeta(clazz: A.class.name, method: 'hi'))
        expect:
        wrapper.target == impl
    }
}
