package org.segment.rpc.server.codec

import groovy.transform.CompileStatic
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.serialize.CompressFactory
import org.segment.rpc.server.serialize.Serializer
import org.segment.rpc.server.serialize.SerializerFactory

@CompileStatic
class RpcMessage {
    MessageType messageType = MessageType.REQ

    Serializer.Type serializeType = Serializer.Type.KYRO

    CompressType compressType = CompressType.NONE

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
        Class clazz = messageType == MessageType.REQ ? Req : Resp

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
        messageType == MessageType.PING || messageType == MessageType.PONG
    }

    RpcMessage response() {
        // use same types as request
        new RpcMessage(serializeType: this.serializeType,
                messageType: this.messageType == MessageType.PING ? MessageType.PONG : MessageType.RESP,
                compressType: this.compressType)
    }

    @CompileStatic
    static enum MessageType {
        REQ(1 as Byte), RESP(2 as Byte), PING(3 as Byte), PONG(4 as Byte), DISCONNECT(-1 as Byte)

        byte value

        MessageType(byte value) {
            this.value = value
        }
    }

    @CompileStatic
    static enum CompressType {
        NONE(1 as Byte), GZIP(2 as Byte), LZ4(3 as Byte), CUSTOM(4 as Byte)

        byte value

        CompressType(byte value) {
            this.value = value
        }
    }

    static MessageType convertMessageType(byte b) {
        if (b == 1) {
            return MessageType.REQ
        }

        if (b == 2) {
            return MessageType.RESP
        }

        if (b == 3) {
            return MessageType.PING
        }

        if (b == 4) {
            return MessageType.PONG
        }

        if (b == -1) {
            return MessageType.DISCONNECT
        }
    }

    static CompressType convertCompressType(byte b) {
        if (b == 1) {
            return CompressType.NONE
        }

        if (b == 2) {
            return CompressType.GZIP
        }

        if (b == 3) {
            return CompressType.LZ4
        }

        if (b == 4) {
            return CompressType.CUSTOM
        }
    }
}
