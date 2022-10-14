package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
abstract class RegexMatchHandler extends AbstractHandler {
    Pattern pattern

    String context

    @Override
    boolean isUriMatch(String uriInput, Req req) {
        if (context != null && !uriInput.startsWith(context)) {
            return false
        }
        String uriToMatch = context != null ? uriInput[context.length()..-1] : uriInput
        uriToMatch ==~ pattern
    }

    @Override
    String name() {
        'regex:' + (context ?: '') + pattern.toString()
    }
}
