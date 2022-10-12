package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

@CompileStatic
class Resp extends HeaderSupport implements Serializable {
    static enum Status {
        OK(200), EMPTY(404), INTERNAL_EX(500)

        int value

        Status(int value) {
            this.value = value
        }
    }

    Resp() {}

    static Resp empty() {
        new Resp(status: Status.EMPTY)
    }

    static Resp one(Object body = null) {
        def resp = new Resp()
        resp.body = body
        resp
    }

    static Resp fail(String message, Status status = null) {
        def resp = new Resp()
        resp.status = status ?: Status.INTERNAL_EX
        resp.message = message
        resp
    }

    Status status = Status.OK

    boolean ok() {
        status == Status.OK
    }

    String uuid

    String message

    Object body
}
