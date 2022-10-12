package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

@CompileStatic
abstract class HeaderSupport {
    protected Map<String, String> headers = [:]
    protected Map<String, Object> attributes = [:]

    HeaderSupport header(String key, String value) {
        headers[key] = value
        this
    }

    String header(String key) {
        headers[key]
    }

    HeaderSupport attr(String key, Object value) {
        attributes[key] = value
        this
    }

    Object attr(String key) {
        attributes[key]
    }
}
