package org.segment.rpc.server.handler

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.segment.rpc.client.ChannelHolder
import org.segment.rpc.manage.ClientChannelInfo
import org.segment.rpc.server.codec.RpcMessage
import org.segment.rpc.server.registry.RemoteUrl
import org.segment.rpc.stats.CounterInMinute
import org.segment.rpc.stats.StatsType

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
@Slf4j
class RpcHandler extends SimpleChannelInboundHandler<RpcMessage> {
    private RemoteUrl remoteUrl
    private ThreadPoolExecutor executor
    private ChannelHolder channelHolder

    RpcHandler(RemoteUrl remoteUrl, ThreadPoolExecutor executor, ChannelHolder channelHolder) {
        this.remoteUrl = remoteUrl
        this.executor = executor
        this.channelHolder = channelHolder
    }

    // for management dashboard
    static Map<String, ClientChannelInfo> clientChannelInfoHolder = new ConcurrentHashMap<>()

    @Override
    void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx)

        String remoteAddress = ctx.channel().remoteAddress().toString()
        clientChannelInfoHolder.put(remoteAddress, new ClientChannelInfo(remoteAddress))
        log.info 'channel register, remove address: {}', remoteAddress
    }

    @Override
    void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx)

        String remoteAddress = ctx.channel().remoteAddress().toString()
        clientChannelInfoHolder.remove(remoteAddress)
        log.info 'channel unregister, remote address: {}', remoteAddress
    }

    @Override
    void channelActive(ChannelHandlerContext ctx) throws Exception {
        def channel = ctx.channel()
        log.info 'channel active {}', channel.remoteAddress()
        channelHolder.add(remoteUrl, channel)

        super.channelActive(ctx)
    }

    @Override
    void channelInactive(ChannelHandlerContext ctx) throws Exception {
        def channel = ctx.channel()
        log.info 'channel inactive, remote address: {}', channel.remoteAddress()
        channelHolder.remove(remoteUrl, channel)

        super.channelInactive(ctx)
    }

    private AtomicInteger count = new AtomicInteger(0)

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
        try {
            executor.execute {
                RpcMessage result = msg.response()
                // PING send not data
                if (!msg.isPingPong()) {
                    msg.bytesToData()
                    Req req = (Req) msg.data
                    def resp = ChainHandler.instance.handle(req)
                    if (resp) {
                        // for future complete
                        resp.uuid = req.uuid
                        result.data = resp
                    } else {
                        def empty = Resp.notFound('not found')
                        empty.uuid = req.uuid
                        result.data = empty
                    }
                    result.dataToBytes()
                }

                writeAndFlush(ctx, result)
            }

            def number = count.incrementAndGet()
            if (number % executor.maximumPoolSize == 0) {
                CounterInMinute.instance.setOne(executor.queue.size(), StatsType.QUEUE_SIZE)
            }
            executor
        } catch (RejectedExecutionException e) {
            CounterInMinute.instance.increaseAndGet(1, StatsType.REJECT_NUMBER)

            RpcMessage result = msg.response()
            // PING send not data
            if (!msg.isPingPong()) {
                msg.bytesToData()
                Req req = (Req) msg.data
                log.warn 'thread pool reject, request id: {}', req.uuid

                def resp = Resp.reject('thread pool reject')
                resp.uuid = req.uuid

                result.data = resp
                result.dataToBytes()
            } else {
                log.warn 'thread pool reject, ping pong'
            }

            writeAndFlush(ctx, result)
        }
    }

    private void writeAndFlush(ChannelHandlerContext ctx, RpcMessage result) {
        def channel = ctx.channel()
        if (channel.isActive() && channel.isWritable()) {
            ctx.writeAndFlush(result).addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
        } else {
            log.warn 'rpc handle fail, channel is not writable, remote address: {}', channel.remoteAddress()
        }
    }

    @Override
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error('rpc handle exception ' + ctx.channel().remoteAddress(), cause)
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