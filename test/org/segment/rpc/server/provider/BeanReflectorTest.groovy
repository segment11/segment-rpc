package org.segment.rpc.server.provider

import spock.lang.Specification

class BeanReflectorTest extends Specification {

    private static interface T1 {
        String hi()

        String hi2(String prefix)
    }

    private static class T1Impl implements T1 {
        String name

        @Override
        String hi() {
            'hi ' + name
        }

        @Override
        String hi2(String prefix) {
            'hi ' + prefix + ' ' + name
        }
    }

    def 'method access'() {
        given:
        def reflector = BeanReflector.get(T1Impl, 'hi')
        def reflector2 = BeanReflector.get(T1Impl, 'hi2', String.class)
        println reflector.returnType
        println reflector2.returnType
        expect:
        BeanReflector.getMaCached(T1Impl) == BeanReflector.getMaCached(T1Impl)

        def target = new T1Impl(name: 'kerry')
        reflector.invoke(target) == 'hi kerry'
        reflector2.invoke(target, 'test') == 'hi test kerry'
    }
}
