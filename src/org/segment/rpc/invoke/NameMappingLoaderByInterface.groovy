package org.segment.rpc.invoke

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class NameMappingLoaderByInterface implements BeanGetByInterface {
    Map<String, String> nameMapping = [:]

    void put(String interfaceName, String className) {
        nameMapping[interfaceName] = className
    }

    @Override
    Object get(String interfaceName) {
        Class.forName(nameMapping[interfaceName]).newInstance()
    }
}
