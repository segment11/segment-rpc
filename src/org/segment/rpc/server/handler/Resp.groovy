package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

@CompileStatic
class Resp implements Serializable {
    @CompileStatic
    static enum Status {
        OK(200), NOT_FOUND(404), INTERNAL_EX(500)

        int value

        Status(int value) {
            this.value = value
        }
    }

    Resp() {}

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

    String uuid

    Status status = Status.OK

    String message

    Object body

    boolean ok() {
        status == Status.OK
    }
}
