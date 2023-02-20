package org.segment.rpc.server.codec

import groovy.transform.CompileStatic
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import org.segment.rpc.stats.CounterInMinute
import org.segment.rpc.stats.StatsType

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
class Encoder extends MessageToByteEncoder<RpcMessage> {
    static int MAGIC_NUMBER_INT = 0

    static {
        'segment'.eachWithIndex { it, int i ->
            def c = it as char
            MAGIC_NUMBER_INT += Character.getNumericValue(c)
        }
    }

    static final byte VERSION = 1
    // put a int
    static final int LEN = 4
    static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024
    static final int MIN_LEN = 16
    // magic + version + fullLength int + messageType + serializeType + compressType + requestId int
    static final int HEADER_LEN = LEN + 1 + LEN + 1 + 1 + 1 + 4

    private AtomicInteger requestIdGenerator = new AtomicInteger(0)

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage message, ByteBuf out) throws Exception {
        out.writeInt MAGIC_NUMBER_INT
        out.writeByte VERSION

        // fully len skip
        out.writerIndex out.writerIndex() + LEN

        out.writeByte message.messageType.value
        out.writeByte message.serializeType.value
        out.writeByte message.compressType.value
        out.writeInt requestIdGenerator.getAndIncrement()

        int fullLen = HEADER_LEN
        def dataBytes = message.dataBytes
        if (!message.isPingPong() && dataBytes != null) {
            out.writeBytes(dataBytes)
            int bodyLen = dataBytes.length
            fullLen += bodyLen
        }

        CounterInMinute.instance.increaseAndGet(fullLen as long, StatsType.ENCODE_LENGTH)

        // fully len set
        int writeIndex = out.writerIndex()
        out.writerIndex writeIndex - fullLen + LEN + 1
        out.writeInt fullLen
        out.writerIndex writeIndex
    }
}
