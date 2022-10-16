package org.segment.rpc.server.handler

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.segment.rpc.manage.ClientChannelInfo
import org.segment.rpc.server.codec.Encoder
import org.segment.rpc.server.codec.RpcMessage
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Slf4j
class RpcHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private RemoteUrl remoteUrl

    RpcHandler(RemoteUrl remoteUrl) {
        this.remoteUrl = remoteUrl
    }

    // for management dashboard
    static Map<String, ClientChannelInfo> clientChannelInfoHolder = new ConcurrentHashMap<>()

    @Override
    void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx)

        String remoteAddress = ctx.channel().remoteAddress().toString()
        clientChannelInfoHolder.put(remoteAddress, new ClientChannelInfo(remoteAddress))
        log.info 'channel register {}', remoteAddress
    }

    @Override
    void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx)

        String remoteAddress = ctx.channel().remoteAddress().toString()
        clientChannelInfoHolder.remove(remoteAddress)
        log.info 'channel unregister {}', remoteAddress
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
        RpcMessage result = msg.response()
        if (msg.messageType == RpcMessage.MessageType.PING) {
            result.data = Encoder.PONG
        } else {
            Req req = msg.data as Req
            def resp = ChainHandler.instance.handle(req)
            if (resp) {
                // for future complete
                resp.uuid = req.uuid
                result.data = resp
            } else {
                def empty = Resp.empty()
                empty.uuid = req.uuid
                result.data = empty
            }
        }

        if (ctx.channel().isActive() && ctx.channel().isWritable()) {
            ctx.writeAndFlush(result).addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
        } else {
            log.warn('rpc handle error as channel not writable {}', ctx.channel().remoteAddress())
        }
    }

    @Override
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error('rpc handle error ' + ctx.channel().remoteAddress(), cause)
        ctx.close()
    }

    @Override
    void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)) {
            super.userEventTriggered(ctx, evt)
            return
        }

        def state = ((IdleStateEvent) evt).state()
        if (state == IdleState.READER_IDLE) {
            log.info('idle checked, so close the connection')
            ctx.close()
        }
    }
}
