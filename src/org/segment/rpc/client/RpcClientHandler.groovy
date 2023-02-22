package org.segment.rpc.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.segment.rpc.server.codec.RpcMessage
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.registry.EventType
import org.segment.rpc.server.registry.Registry
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
@Slf4j
class RpcClientHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private ResponseFutureHolder responseFutureHolder
    private ChannelHolder channelHolder
    private Registry registry

    RpcClientHandler(ResponseFutureHolder responseFutureHolder, ChannelHolder channelHolder, Registry registry) {
        this.responseFutureHolder = responseFutureHolder
        this.channelHolder = channelHolder
        this.registry = registry
    }

    private AtomicInteger count = new AtomicInteger(0)

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
        if (msg.messageType == RpcMessage.MessageType.PONG) {
            def loopCount = count.incrementAndGet()
            if (loopCount % 10 == 0) {
                log.info 'heartbeat, loop count: {}', loopCount
            }
            return
        }

        if (msg.messageType == RpcMessage.MessageType.DISCONNECT) {
            this.exceptionCaught(ctx, new RemoteUrlDownException('receive disconnect message'))
            return
        }

        if (msg.messageType != RpcMessage.MessageType.RESP) {
            return
        }

        msg.bytesToData()
        Resp resp = (Resp) msg.data
        responseFutureHolder.complete(resp)
    }

    @Override
    void channelActive(ChannelHandlerContext ctx) throws Exception {
        def channel = ctx.channel()
        InetSocketAddress localAddress = channel.localAddress() as InetSocketAddress
        InetSocketAddress socketAddress = channel.remoteAddress() as InetSocketAddress
        def address = socketAddress.address
        def remoteUrl = new RemoteUrl(address.hostAddress, socketAddress.port)
        log.info 'channel active, local address: {}, remote server: {}', localAddress, remoteUrl
        registry.fire(remoteUrl, EventType.ACTIVE)

        super.channelActive(ctx)
    }

    @Override
    void channelInactive(ChannelHandlerContext ctx) throws Exception {
        def channel = ctx.channel()
        InetSocketAddress localAddress = channel.localAddress() as InetSocketAddress
        InetSocketAddress socketAddress = channel.remoteAddress() as InetSocketAddress
        def address = socketAddress.address
        def remoteUrl = new RemoteUrl(address.hostAddress, socketAddress.port)
        log.info 'channel inactive, local address: {}, remote server: {}', localAddress, remoteUrl

        // if all channel is inactive, fire event, so that the registry(discover) will set ready false
        def isLeftActive = channelHolder.isLeftActive(remoteUrl, channel)
        if (!isLeftActive) {
            registry.fire(remoteUrl, EventType.INACTIVE)
        }

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

        Channel channel = ctx.channel()
        log.debug('write idle, remote address: {}, do ping', channel.remoteAddress())
        def msg = new RpcMessage()
        msg.messageType = RpcMessage.MessageType.PING
        channel.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
    }

    @Override
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        def isDisconnect = cause instanceof RemoteUrlDownException

        def channel = ctx.channel()
        InetSocketAddress socketAddress = channel.remoteAddress() as InetSocketAddress
        if (!isDisconnect) {
            log.error('client handle exception - ' + socketAddress, cause)
        } else {
            log.warn 'client handle disconnect - ' + socketAddress + ' - ' + cause.message
        }
        ctx.close()

        def address = socketAddress.address
        def remoteUrl = new RemoteUrl(address.hostAddress, socketAddress.port)

        def isLeftActive = isDisconnect ? false : channelHolder.isLeftActive(remoteUrl, channel)
        if (!isLeftActive) {
            registry.fire(remoteUrl, EventType.INACTIVE)
            channelHolder.remove(remoteUrl)
            log.info 'remove from channel holder, remote server: {}', remoteUrl
        }
    }
}
