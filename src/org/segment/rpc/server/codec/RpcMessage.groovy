package org.segment.rpc.server.codec

import groovy.transform.CompileStatic
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.serialize.Compress
import org.segment.rpc.server.serialize.CompressFactory
import org.segment.rpc.server.serialize.Serializer
import org.segment.rpc.server.serialize.SerializerFactory

@CompileStatic
class RpcMessage {
    Type messageType = Type.REQ

    Serializer.Type serializeType = Serializer.Type.KYRO

    Compress.Type compressType = Compress.Type.NONE

    int requestId

    Serializable data

    byte[] dataBytes

    // dot not run in netty io threads
    void dataToBytes() {
        if (data == null) {
            return
        }

        def serializer = SerializerFactory.create(serializeType)
        def compress = CompressFactory.create(compressType)
        def os = new ByteArrayOutputStream()

        if (compress == null) {
            serializer.write(data, os)
        } else {
            def pipeIn = new PipedInputStream()
            def pipeOut = new PipedOutputStream(pipeIn)

            serializer.write(data, pipeOut)
            compress.compress(pipeIn, os)
        }

        dataBytes = os.toByteArray()
    }

    void bytesToData() {
        if (dataBytes == null) {
            return
        }

        def compress = CompressFactory.create(compressType)
        def serializer = SerializerFactory.create(serializeType)
        Class clazz = messageType == Type.REQ ? Req : Resp

        def is = new ByteArrayInputStream(dataBytes)

        if (compress == null) {
            data = serializer.read(is, clazz)
        } else {
            def pipeIn = new PipedInputStream()
            def pipeOut = new PipedOutputStream(pipeIn)
            compress.decompress(is, pipeOut)
            data = serializer.read(pipeIn, clazz)
        }
    }

    boolean isPingPong() {
        messageType == Type.PING || messageType == Type.PONG
    }

    RpcMessage response() {
        // use same types as request
        new RpcMessage(serializeType: this.serializeType,
                messageType: this.messageType == Type.PING ? Type.PONG : Type.RESP,
                compressType: this.compressType)
    }

    @CompileStatic
    static enum Type {
        REQ(1 as Byte), RESP(2 as Byte), PING(3 as Byte), PONG(4 as Byte), DISCONNECT(-1 as Byte)

        byte value

        Type(byte value) {
            this.value = value
        }

        static Type convert(byte b) {
            if (b == 1) {
                return REQ
            }

            if (b == 2) {
                return RESP
            }

            if (b == 3) {
                return PING
            }

            if (b == 4) {
                return PONG
            }

            if (b == -1) {
                return DISCONNECT
            }
        }
    }
}
