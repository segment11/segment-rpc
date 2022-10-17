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
import org.segment.rpc.stats.CounterInMinute
import org.segment.rpc.stats.StatsType

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor

@CompileStatic
@Slf4j
class RpcHandler extends SimpleChannelInboundHandler<RpcMessage> {
    private RemoteUrl remoteUrl
    private ThreadPoolExecutor executor

    RpcHandler(RemoteUrl remoteUrl, ThreadPoolExecutor executor) {
        this.remoteUrl = remoteUrl
        this.executor = executor
    }

    // for management dashboard
    static Map<String, ClientChannelInfo> clientChannelInfoHolder = new ConcurrentHashMap<>()

    @Override
    void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx)

        String remoteAddress = ctx.channel().remoteAddress().toString()
        clientChannelInfoHolder.put(remoteAddress, new ClientChannelInfo(address: remoteAddress))
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
        try {
            executor.submit {
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

                writeAndFlush(ctx, result)
            }
        } catch (RejectedExecutionException e) {
            CounterInMinute.instance.increaseAndGet(1, StatsType.REJECT_NUMBER)

            if (!msg.isPingPong()) {
                Req req = msg.data as Req
                log.warn('thread pool reject, request id {}', req.uuid)
            }

            RpcMessage result = msg.response()
            result.data = Resp.fail('thread pool reject')

            writeAndFlush(ctx, result)
        }
    }

    private void writeAndFlush(ChannelHandlerContext ctx, RpcMessage result) {
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