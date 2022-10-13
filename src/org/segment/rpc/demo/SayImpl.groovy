package org.segment.rpc.demo

import groovy.transform.CompileStatic

@CompileStatic
class SayImpl implements SayInterface {
    String hi(String name) {
        'hi - ' + name
    }
}
