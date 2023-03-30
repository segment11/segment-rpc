package org.segment.rpc.server.codec

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.serialize.Compress
import org.segment.rpc.server.serialize.CompressFactory
import org.segment.rpc.server.serialize.Serializer
import org.segment.rpc.server.serialize.SerializerFactory
import org.segment.rpc.stats.CounterInMinute
import org.segment.rpc.stats.StatsType

import static org.segment.rpc.server.codec.Encoder.*
import static org.segment.rpc.server.codec.RpcMessage.Type

@CompileStatic
@Slf4j
class Decoder extends LengthFieldBasedFrameDecoder {
    Decoder() {
        // + 1(version byte length 1)
        super(MAX_FRAME_LENGTH,
                INT_LEN + 1,
                INT_LEN,
                -(INT_LEN + 1 + INT_LEN),
                0)
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        Object decoded = super.decode(ctx, byteBuf)
        if (!(decoded instanceof ByteBuf)) {
            return decoded
        }

        def frame = decoded as ByteBuf
        if (frame.readableBytes() < HEADER_LEN) {
            throw new IllegalArgumentException('frame length less than header length')
        }

        try {
            return decodeFrame(frame)
        } catch (Exception e) {
            log.error 'decode frame error', e
            throw e
        } finally {
            frame.release()
        }
    }

    private RpcMessage decodeFrame(ByteBuf frame) {
        checkMagicNumber(frame)
        checkVersion(frame)

        int fullLen = frame.readInt()
        CounterInMinute.instance.increaseAndGet(fullLen as long, StatsType.DECODE_LENGTH)

        byte messageType = frame.readByte()
        byte serializeType = frame.readByte()
        byte compressType = frame.readByte()
        int requestId = frame.readInt()

        RpcMessage message = new RpcMessage(
                messageType: RpcMessage.Type.convert(messageType),
                serializeType: Serializer.Type.convert(serializeType),
                compressType: Compress.Type.convert(compressType),
                requestId: requestId
        )
        if (messageType == Type.PING.value) {
            return message
        }

        if (messageType == Type.PONG.value) {
            return message
        }

        int bodyLen = fullLen - HEADER_LEN
        if (bodyLen > 0) {
            def compress = CompressFactory.create(message.compressType)
            // no compress, need not memory copy
            if (compress == null) {
                def is = new ByteBufInputStream(frame.readSlice(bodyLen))

                def serializer = SerializerFactory.create(message.serializeType)
                Class clazz = messageType == RpcMessage.Type.REQ.value ? Req : Resp
                message.data = serializer.read(is, clazz)
            } else {
                byte[] bodyBytes = new byte[bodyLen]
                frame.readBytes(bodyBytes)
                message.dataBytes = bodyBytes
            }
        }
        message
    }

    private void checkMagicNumber(ByteBuf frame) {
        def readInt = frame.readInt()
        if (MAGIC_NUMBER_INT != readInt) {
            throw new IllegalArgumentException('magic number not match - ' + readInt)
        }
    }

    private void checkVersion(ByteBuf frame) {
        def versionByte = frame.readByte()
        if (versionByte != VERSION) {
            throw new IllegalArgumentException('version not match - ' + versionByte)
        }
    }
}
