package org.segment.rpc.demo

import groovy.transform.CompileStatic

@CompileStatic
interface SayInterface {
    String hi(String name)
}
