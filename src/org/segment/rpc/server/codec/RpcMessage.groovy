package org.segment.rpc.server.codec

import groovy.transform.CompileStatic
import org.segment.rpc.server.serialize.Serializer

@CompileStatic
class RpcMessage {
    Serializer.Type serializeType = Serializer.Type.KYRO

    MessageType messageType = MessageType.REQ

    CompressType compressType = CompressType.NONE

    int requestId

    Serializable data

    boolean isPingPong() {
        messageType == MessageType.PING || messageType == MessageType.PONG
    }

    RpcMessage response() {
        // use same types as request
        new RpcMessage(serializeType: this.serializeType,
                messageType: this.messageType == MessageType.PING ? MessageType.PONG : MessageType.RESP,
                compressType: this.compressType)
    }

    static enum MessageType {
        REQ(1 as Byte), RESP(2 as Byte), PING(3 as Byte), PONG(4 as Byte)

        byte value

        MessageType(byte value) {
            this.value = value
        }
    }

    static enum CompressType {
        NONE(1 as Byte), GZIP(2 as Byte), LZ(3 as Byte)

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
    }

    static CompressType convertCompressType(byte b) {
        if (b == 1) {
            return CompressType.NONE
        }

        if (b == 2) {
            return CompressType.GZIP
        }

        if (b == 3) {
            return CompressType.LZ
        }
    }
}
