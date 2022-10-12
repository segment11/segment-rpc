package org.segment.rpc.server.codec

import groovy.transform.CompileStatic
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import org.segment.rpc.server.serialize.CompressFactory
import org.segment.rpc.server.serialize.SerializerFactory

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
class Encoder extends MessageToByteEncoder<RpcMessage> {
    static final byte[] MAGIC_NUMBER = new byte['segment'.length()]

    static {
        'segment'.eachWithIndex { it, int i ->
            def c = it as char
            def b = c as byte
            MAGIC_NUMBER[i] = b
        }
    }

    static final byte VERSION = 1
    // put a int
    static final int LEN = 4
    static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024
    static final int MIN_LEN = 16
    // magic + version + fullLength int + messageType + serializeType + compressType + requestId int
    static final int HEADER_LEN = MAGIC_NUMBER.length + 1 + LEN + 1 + 1 + 1 + 4

    static final String PING = 'ping'
    static final String PONG = 'pong'

    private AtomicInteger requestIdGenerator = new AtomicInteger(0)

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage message, ByteBuf out) throws Exception {
        out.writeBytes MAGIC_NUMBER
        out.writeByte VERSION

        // fully len skip
        out.writerIndex out.writerIndex() + LEN

        out.writeByte message.messageType.value
        out.writeByte message.serializeType.value
        out.writeByte message.compressType.value
        out.writeInt requestIdGenerator.getAndIncrement()

        int fullLen = HEADER_LEN
        if (!message.isPingPong()) {
            byte[] data = SerializerFactory.create(message.serializeType).write(message.data)
            if (message.compressType != RpcMessage.CompressType.NONE) {
                def dataTmp = CompressFactory.create(message.compressType).compress(data)
                fullLen += dataTmp.length
                out.writeBytes dataTmp
            } else {
                fullLen += data.length
                out.writeBytes data
            }
        }

        // fully len set
        int writeIndex = out.writerIndex()
        out.writerIndex writeIndex - fullLen + MAGIC_NUMBER.length + 1
        out.writeInt fullLen
        out.writerIndex writeIndex
    }
}
