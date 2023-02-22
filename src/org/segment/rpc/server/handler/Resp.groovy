package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

@CompileStatic
class Resp implements Serializable {
    @CompileStatic
    static enum Status {
        OK(200), NOT_FOUND(404), REJECT(403), INTERNAL_EX(500)

        int value

        Status(int value) {
            this.value = value
        }
    }

    Resp() {}

    static Resp one(Object body = null) {
        def resp = new Resp()
        resp.status = Status.OK
        resp.body = body
        resp
    }

    static Resp fail(String message, Status status = null) {
        def resp = new Resp()
        resp.status = status ?: Status.INTERNAL_EX
        resp.message = message
        resp
    }

    static Resp notFound(String message) {
        fail(message, Status.NOT_FOUND)
    }

    static Resp reject(String message) {
        fail(message, Status.REJECT)
    }

    String uuid

    Status status

    String message

    Object body

    boolean ok() {
        status == Status.OK
    }
}
