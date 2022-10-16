package org.segment.rpc.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.segment.rpc.server.codec.Encoder
import org.segment.rpc.server.codec.RpcMessage
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.registry.EventHandler
import org.segment.rpc.server.registry.EventType
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
@Slf4j
class RpcClientHandler extends SimpleChannelInboundHandler<RpcMessage> {

    AtomicInteger count = new AtomicInteger(0)

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
        if (msg.messageType == RpcMessage.MessageType.PONG) {
            count.getAndIncrement()
            def number = count.get()
            if (number % 10 == 0) {
                log.info 'heartbeat {}, count {}', msg.data, number
            }
            return
        }

        if (msg.messageType != RpcMessage.MessageType.RESP) {
            return
        }

        Resp resp = msg.data as Resp
        ProcessFuture.instance.complete(resp)
    }

    @Override
    void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socketAddress = ctx.channel().remoteAddress() as InetSocketAddress
        def address = socketAddress.address
        def remoteUrl = new RemoteUrl(address.hostAddress, socketAddress.port)
        log.info 'channel active {}', remoteUrl
        EventHandler.instance.fire(remoteUrl, EventType.ACTIVE)

        super.channelActive(ctx)
    }

    @Override
    void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socketAddress = ctx.channel().remoteAddress() as InetSocketAddress
        def address = socketAddress.address
        def remoteUrl = new RemoteUrl(address.hostAddress, socketAddress.port)
        log.info 'channel inactive {}', remoteUrl
        EventHandler.instance.fire(remoteUrl, EventType.INACTIVE)

        super.channelInactive(ctx)
    }

    @Override
    void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)) {
            super.userEventTriggered(ctx, evt)
            return
        }

        def state = ((IdleStateEvent) evt).state()
        if (state != IdleState.WRITER_IDLE) {
            return
        }
        log.debug('write idle {}, do ping', ctx.channel().remoteAddress())

        Channel channel = ctx.channel()
        def msg = new RpcMessage()
        msg.messageType = RpcMessage.MessageType.PING
        msg.data = Encoder.PING
        channel.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
    }

    @Override
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error('client handle exception - ' + ctx.channel().remoteAddress(), cause)
        ctx.close()
    }
}
