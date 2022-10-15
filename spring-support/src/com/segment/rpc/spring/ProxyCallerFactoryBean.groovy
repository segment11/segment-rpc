package com.segment.rpc.spring

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.client.RpcClient
import org.segment.rpc.invoke.ProxyCreator
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.BeansException
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

@CompileStatic
class ProxyCallerFactoryBean {
    Object create() {
        new CallerBeanWapper()
    }

    @CompileStatic
    @Slf4j
    static class CallerBeanWapper extends BeanWrapperImpl implements FactoryBean, ApplicationContextAware {

        String interfaceClassName
        String context
        String clientBeanRefer

        private ApplicationContext applicationContext

        @Override
        void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = applicationContext
        }

        @Override
        Object getObject() throws Exception {
            def client = applicationContext.getBean(clientBeanRefer)
            if (client == null || !(client instanceof RpcClient)) {
                log.warn 'client bean not found not a rpc client'
                return null
            }

            RpcClient c = client as RpcClient
            new ProxyCreator(c, context).create(objectType)
        }

        @Override
        Class<?> getObjectType() {
            applicationContext.classLoader.loadClass(interfaceClassName)
        }

        @Override
        boolean isSingleton() {
            return true
        }
    }
}
