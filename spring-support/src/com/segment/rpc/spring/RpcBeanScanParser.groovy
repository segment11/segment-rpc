package com.segment.rpc.spring

import groovy.transform.CompileStatic
import org.springframework.beans.factory.xml.XmlReaderContext
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser

@CompileStatic
class RpcBeanScanParser extends ComponentScanBeanDefinitionParser {

    @Override
    protected ClassPathBeanDefinitionScanner createScanner(XmlReaderContext readerContext, boolean useDefaultFilters) {
        new RpcMethodProviderScanner(readerContext.getRegistry(), useDefaultFilters,
                readerContext.getEnvironment(), readerContext.getResourceLoader())
    }
}
