package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
abstract class RegexMatchHandler extends AbstractHandler {
    Pattern pattern

    String uriPre

    @Override
    boolean isUriMatch(String uriInput, Req req) {
        if (uriPre != null && !uriInput.startsWith(uriPre)) {
            return false
        }
        String uriToMatch = uriPre != null ? uriInput[uriPre.length()..-1] : uriInput
        uriToMatch ==~ pattern
    }

    @Override
    String name() {
        'regex:' + (uriPre ?: '') + pattern.toString()
    }
}
