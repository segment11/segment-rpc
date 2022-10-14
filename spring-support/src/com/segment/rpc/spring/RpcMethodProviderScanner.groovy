package com.segment.rpc.spring

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.server.provider.BeanCreator
import org.segment.rpc.server.provider.DefaultProvider
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader

@CompileStatic
@Slf4j
class RpcMethodProviderScanner extends ClassPathBeanDefinitionScanner {
    RpcMethodProviderScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
                             Environment environment, ResourceLoader resourceLoader) {
        super(registry, useDefaultFilters, environment, resourceLoader)
    }

    @Override
    protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
        super.postProcessBeanDefinition(beanDefinition, beanName)

        beanDefinition.resolveBeanClass(resourceLoader.classLoader)
        def beanClass = beanDefinition.beanClass
        def annotation = beanClass.getAnnotation(RpcMethodProvider.class)
        if (annotation == null) {
            return
        }

        ApplicationContext context = resourceLoader as ApplicationContext

        Class clazz = annotation.interfaceClass()
        DefaultProvider.instance.provide(clazz, new BeanCreator() {
            @Override
            Object create() {
                context.getBean(clazz)
            }
        })
        log.info 'add bean provide - {}', clazz.name
    }
}
