package com.segment.rpc.spring

import java.lang.annotation.*

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@interface RpcMethodProvider {
    Class interfaceClass()
}
