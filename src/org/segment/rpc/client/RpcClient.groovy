package org.segment.rpc.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.timeout.IdleStateHandler
import org.segment.rpc.common.Conf
import org.segment.rpc.common.SpiSupport
import org.segment.rpc.server.codec.Decoder
import org.segment.rpc.server.codec.Encoder
import org.segment.rpc.server.codec.RpcMessage
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.registry.Referer
import org.segment.rpc.server.registry.Registry
import org.segment.rpc.server.registry.RemoteUrl
import org.segment.rpc.server.serialize.Serializer

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@CompileStatic
@Slf4j
class RpcClient {
    private Conf c = Conf.instance

    private EventLoopGroup eventLoopGroup
    private final Bootstrap bootstrap

    private Registry registry
    private LoadBalance loadBalance

    void stop() {
        if (registry) {
            registry.shutdown()
        }
        ChannelHolder.instance.disconnect()
        eventLoopGroup.shutdownGracefully()
        log.info('event loop group shutdown...')
    }

    RpcClient() {
        c.load()
        log.info c.toString()
        registry = SpiSupport.getRegistry()
        registry.init()
        loadBalance = SpiSupport.getLoadBalance()
        loadBalance.init()

        eventLoopGroup = new NioEventLoopGroup()
        bootstrap = new Bootstrap()

        long writerIdleTimeSeconds = c.getInt('client.writer.idle.seconds', 10) as long

        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        c.getInt('client.connect.timeout.millis', 5000))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new IdleStateHandler(0, writerIdleTimeSeconds, 0, TimeUnit.SECONDS))
                                .addLast(new Encoder())
                                .addLast(new Decoder())
                                .addLast(new RpcClientHandler(RpcClient.this))
                    }
                })
    }

    Channel doConnect(RemoteUrl remoteUrl) {
        def address = remoteUrl.address()
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>()
        bootstrap.connect(address).addListener({ ChannelFuture future ->
            if (future.isSuccess()) {
                log.info 'client connect to remote server ok {}', address.toString()
                completableFuture.complete(future.channel())
            } else {
                completableFuture.completeExceptionally(future.cause())
            }
        } as ChannelFutureListener)
        completableFuture.get()
    }

    Resp sendSync(Req req) {
        send(req).get()
    }

    CompletableFuture<Resp> send(Req req) {
        def referer = loadBalance.select(registry.discover(req), req)
        def channel = getChannelByRefer(referer)
        if (!channel.isActive()) {
            throw new IllegalStateException('channel not active - ' + referer.remoteUrl)
        }

        def msg = new RpcMessage()
        msg.data = req
        msg.messageType = RpcMessage.MessageType.REQ
        initRpcMessage(msg)

        def resultFuture = new CompletableFuture<Resp>()
        ProcessFuture.instance.put(req.uuid, resultFuture)

        channel.writeAndFlush(msg).addListener({ ChannelFuture future ->
            if (!future.isSuccess()) {
                log.error 'send request error - ' + referer.remoteUrl, future.cause()
                resultFuture.completeExceptionally(future.cause())
                future.channel().close()
            } else {
                log.debug('send request ok {}', msg)
            }
        } as ChannelFutureListener)
        resultFuture
    }

    void initRpcMessage(RpcMessage msg) {
        if (c.isOn('client.send.request.use.gzip')) {
            msg.compressType = RpcMessage.CompressType.GZIP
        }
        msg.serializeType = Serializer.Type.KYRO
        if (c.isOn('client.send.request.serialize.type.use.hessian')) {
            msg.serializeType = Serializer.Type.HESSIAN
        }
    }

    Channel getChannelByRefer(Referer referer) {
        def r = ChannelHolder.instance.get(referer.remoteUrl)
        if (r != null) {
            return r
        }
        def newOne = doConnect(referer.remoteUrl)
        ChannelHolder.instance.put(referer.remoteUrl, newOne)
        newOne
    }
}
