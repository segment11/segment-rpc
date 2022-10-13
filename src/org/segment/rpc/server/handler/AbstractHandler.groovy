package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

@CompileStatic
abstract class AbstractHandler implements Handler {
    String uri

    @Override
    String name() {
        uri
    }

    @Override
    Resp handle(Req req) {
        String uriInput = req.uri
        if (!isUriMatch(uriInput, req)) {
            return null
        }

        hi(req)
    }

    abstract Resp hi(Req req)

    boolean isUriMatch(String uriInput, Req req = null) {
        if (uri == uriInput) {
            return true
        }

        def arr = uri.split(/\//)
        def arr2 = uriInput.split(/\//)


        if (arr.length != arr2.length) {
            if (arr[-1] != '**') {
                return false
            }
            for (int i = 0; i < arr.length; i++) {
                if (arr2.length <= i || (arr[i] != '*' && arr[i] != '**' && arr[i] != arr2[i])) {
                    return false
                }
            }
            return true
        } else {
            for (int i = 0; i < arr.length; i++) {
                def s = arr[i]
                if (s == '*' || s == '**') {
                    continue
                }
                if (s.startsWith(':')) {
                    String value = arr2[i]
                    if (req != null) {
                        req.attr(s, value)
                    }
                    continue
                }
                if (s != arr2[i]) {
                    return false
                }
            }
            return true
        }
    }
}
