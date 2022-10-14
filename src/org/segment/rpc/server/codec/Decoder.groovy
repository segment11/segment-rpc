package org.segment.rpc.server.codec

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.serialize.CompressFactory
import org.segment.rpc.server.serialize.Serializer
import org.segment.rpc.server.serialize.SerializerFactory

import static org.segment.rpc.server.codec.Encoder.*
import static org.segment.rpc.server.codec.RpcMessage.CompressType
import static org.segment.rpc.server.codec.RpcMessage.MessageType

@CompileStatic
@Slf4j
class Decoder extends LengthFieldBasedFrameDecoder {
    private Stats stats = StatsHolder.instance.decoderStats

    Decoder() {
        // + 1(version byte length 1)
        super(MAX_FRAME_LENGTH,
                MAGIC_NUMBER.size() + 1,
                LEN,
                -(MAGIC_NUMBER.size() + 1 + LEN),
                0)
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        Object decoded = super.decode(ctx, byteBuf)
        if (!(decoded instanceof ByteBuf)) {
            return decoded
        }

        def frame = decoded as ByteBuf
        if (frame.readableBytes() < MIN_LEN) {
            return decoded
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
        stats.add(fullLen)

        byte messageType = frame.readByte()
        byte serializeType = frame.readByte()
        byte compressType = frame.readByte()
        int requestId = frame.readInt()

        RpcMessage message = new RpcMessage(
                messageType: RpcMessage.convertMessageType(messageType),
                serializeType: Serializer.Type.convert(serializeType),
                compressType: RpcMessage.convertCompressType(compressType),
                requestId: requestId
        )
        if (messageType == MessageType.PING.value) {
            message.data = Encoder.PING
            return message
        }

        if (messageType == MessageType.PONG.value) {
            message.data = Encoder.PONG
            return message
        }

        int bodyLen = fullLen - HEADER_LEN
        if (bodyLen > 0) {
            byte[] body = new byte[bodyLen]
            frame.readBytes(body)

            byte[] bodyTmp = body
            if (compressType != CompressType.NONE.value) {
                bodyTmp = CompressFactory.create(message.compressType).decompress(body)
            }

            def serializer = SerializerFactory.create(message.serializeType)
            if (messageType == MessageType.REQ.value) {
                message.data = serializer.read(bodyTmp, Req)
            } else if (messageType == MessageType.RESP.value) {
                message.data = serializer.read(bodyTmp, Resp)
            }
        }
        message
    }

    private void checkMagicNumber(ByteBuf frame) {
        byte[] tmp = new byte[MAGIC_NUMBER.length]
        frame.readBytes(tmp)
        for (int i = 0; i < tmp.length; i++) {
            if (tmp[i] != MAGIC_NUMBER[i]) {
                throw new IllegalArgumentException('magic number not match - ' + Arrays.toString(tmp))
            }
        }
    }

    private void checkVersion(ByteBuf frame) {
        def versionByte = frame.readByte()
        if (versionByte != VERSION) {
            throw new IllegalArgumentException('version not match - ' + versionByte)
        }
    }
}
