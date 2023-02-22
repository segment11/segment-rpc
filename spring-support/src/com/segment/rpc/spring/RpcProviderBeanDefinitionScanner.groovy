package com.segment.rpc.spring

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.server.provider.DefaultProvider
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader

@CompileStatic
@Slf4j
class RpcProviderBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {
    RpcProviderBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
                                     Environment environment, ResourceLoader resourceLoader) {
        super(registry, useDefaultFilters, environment, resourceLoader)
    }

    @Override
    protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
        super.postProcessBeanDefinition(beanDefinition, beanName)

        ApplicationContext context = resourceLoader as ApplicationContext

        beanDefinition.resolveBeanClass(resourceLoader.classLoader)
        def beanClass = beanDefinition.beanClass

        def annotation = beanClass.getAnnotation(RpcMethodProvider.class)
        if (annotation == null) {
            return
        }

        Class clazz = annotation.interfaceClass()
        DefaultProvider.instance.provide(clazz, new ContextLazyBeanCreator(context, clazz))
        log.info 'add bean provide, class name: {}', clazz.name
    }
}
