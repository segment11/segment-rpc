package com.segment.rpc.spring

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.beans.factory.xml.BeanDefinitionParser
import org.springframework.beans.factory.xml.ParserContext
import org.w3c.dom.Element

@CompileStatic
@Slf4j
class RpcCallerBeanParser implements BeanDefinitionParser {
    final static String BEAN_NAME_SUFFIX = 'Remote'
    final static String FACTORY_BEAN_NAME = 'proxyCallerFactoryBean'
    final static String FACTORY_BEAN_CLASS_NAME = 'com.segment.rpc.spring.ProxyCallerFactoryBean'

    @Override
    BeanDefinition parse(Element element, ParserContext parserContext) {
        String interfaceClassName = element.getAttribute('interface')
        String clientBeanRefer = element.getAttribute('client')
        String context = element.getAttribute('context')
        if (!interfaceClassName || !clientBeanRefer || !context) {
            throw new IllegalArgumentException('interface/client/context required')
        }

        if (!parserContext.registry.containsBeanDefinition(FACTORY_BEAN_NAME)) {
            def fbd = new RootBeanDefinition()
            fbd.setLazyInit(true)
            fbd.setScope(fbd.SCOPE_SINGLETON)
            fbd.beanClassName = FACTORY_BEAN_CLASS_NAME
            parserContext.registry.registerBeanDefinition(FACTORY_BEAN_NAME, fbd)
            log.info 'add proxy caller factory bean'
        }

        def nameSimple = interfaceClassName.split(/\./)[-1]
        // Bean -> beanRemote
        def beanName = nameSimple[0].toLowerCase() + nameSimple[1..-1] + BEAN_NAME_SUFFIX
        if (!parserContext.registry.containsBeanDefinition(beanName)) {
            def bd = new RootBeanDefinition()
            bd.setLazyInit(true)
            bd.setScope(bd.SCOPE_SINGLETON)

            bd.factoryBeanName = FACTORY_BEAN_NAME
            bd.factoryMethodName = 'create'
            def values = bd.propertyValues
            values.addPropertyValue('interfaceClassName', interfaceClassName)
            values.addPropertyValue('clientBeanRefer', clientBeanRefer)
            values.addPropertyValue('context', context)

            parserContext.registry.registerBeanDefinition(beanName, bd)
            log.info 'define proxy caller bean: {}, interface: {}, context: {}', beanName, interfaceClassName, context
        }

        null
    }
}
