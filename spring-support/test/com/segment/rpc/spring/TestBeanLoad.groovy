package com.segment.rpc.spring


import org.segment.rpc.server.provider.DefaultProvider
import org.springframework.context.support.GenericGroovyApplicationContext
import spock.lang.Specification

class TestBeanLoad extends Specification {

    def testAll() {
        given:
        def context = new GenericGroovyApplicationContext()
        context.load('classpath*:beans.groovy')
        context.refresh()
        and:
        def metaList = DefaultProvider.instance.listMethodsByInterface(Bean.class)
        println metaList
        def bean = context.getBean('beanImpl')
        expect:
        metaList.size() == 1
        bean != null
        bean == DefaultProvider.instance.lookupMethod(metaList[0]).target
        cleanup:
        context.close()
    }
}
