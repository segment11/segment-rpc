package org.segment.rpc.server

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.concurrent.DefaultEventExecutorGroup
import org.segment.rpc.common.Conf
import org.segment.rpc.common.SpiSupport
import org.segment.rpc.common.Utils
import org.segment.rpc.server.codec.Decoder
import org.segment.rpc.server.codec.Encoder
import org.segment.rpc.server.handler.RpcHandler
import org.segment.rpc.server.registry.Registry

import java.util.concurrent.TimeUnit

@CompileStatic
@Slf4j
class RpcServer {

    private Conf c = Conf.instance

    private EventLoopGroup workerGroup

    private EventLoopGroup bossGroup

    private DefaultEventExecutorGroup handlerGroup

    private Registry registry = SpiSupport.getRegistry()

    void stop() {
        if (registry) {
            registry.shutdown()
        }
        if (workerGroup) {
            workerGroup.shutdownGracefully()
            log.info('worker group shutdown...')
        }
        if (bossGroup) {
            bossGroup.shutdownGracefully()
            log.info('boss group shutdown...')
        }
        if (handlerGroup) {
            handlerGroup.shutdownGracefully()
            log.info('handler group shutdown...')
        }
    }

    void start() {
        int port = c.getInt('server.listen.port', 8877)

        def cpuNumber = Runtime.getRuntime().availableProcessors()
        int handleGroupThreadNumber = c.getInt('server.handle.group.thread.number', cpuNumber * 2)

        handlerGroup = new DefaultEventExecutorGroup(handleGroupThreadNumber,
                Utils.createThreadFactory('service-handler-group', false)
        )

        workerGroup = new NioEventLoopGroup()
        bossGroup = new NioEventLoopGroup()
        def bootstrap = new ServerBootstrap()
        try {
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
                                    .addLast(new Encoder())
                                    .addLast(new Decoder())
                                    .addLast(handlerGroup, new RpcHandler())
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)

            String host = Utils.localIp()
            log.info('server ready to start {}:{}', host, port)

            def future = bootstrap.bind(host, port).sync()
            future.channel().closeFuture().sync()
        } catch (InterruptedException e) {
            log.error('server start interrupted error', e)
        } finally {
            stop()
        }
    }
}