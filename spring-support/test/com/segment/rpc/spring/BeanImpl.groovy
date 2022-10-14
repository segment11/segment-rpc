package com.segment.rpc.spring

import groovy.transform.CompileStatic
import org.springframework.stereotype.Component

@CompileStatic
@Component
@RpcMethodProvider(interfaceClass = Bean.class)
class BeanImpl implements Bean {
    @Override
    void hi(String name) {
        println 'hi - ' + name
    }
}
