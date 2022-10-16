package org.segment.rpc.server

import com.google.common.util.concurrent.ThreadFactoryBuilder
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
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports
import org.segment.rpc.common.RpcConf
import org.segment.rpc.common.SpiSupport
import org.segment.rpc.common.Utils
import org.segment.rpc.invoke.MethodInvokeHandler
import org.segment.rpc.invoke.ProxyCreator
import org.segment.rpc.server.codec.Decoder
import org.segment.rpc.server.codec.Encoder
import org.segment.rpc.server.codec.StatsHolder
import org.segment.rpc.server.handler.ChainHandler
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.handler.RpcHandler
import org.segment.rpc.server.provider.DefaultProvider
import org.segment.rpc.server.registry.Registry
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.TimeUnit

@CompileStatic
@Slf4j
class RpcServer {

    private RpcConf c

    private EventLoopGroup workerGroup

    private EventLoopGroup bossGroup

    private DefaultEventExecutorGroup handlerGroup

    private Registry registry

    private RemoteUrl remoteUrl

    private HTTPServer metricsServer

    RpcServer(RpcConf conf = null) {
        this.c = conf ?: RpcConf.fromLoad()
        log.info c.toString()

        String host = c.getString('server.listen.host', Utils.localIp())
        int port = c.getInt('server.listen.port', 8877)

        remoteUrl = new RemoteUrl(host, port)
        if (c.isOn('server.metric.export')) {
            remoteUrl.metricExportPort = c.getInt('server.metric.listen.port', 8878)
        }
        remoteUrl.context = c.getString('server.registry.context', '/rpc')
        // first you can set ready false, then use segment-rpc-manager change weight and ready flag
        remoteUrl.ready = Boolean.valueOf(c.getString('server.ready', 'true'))
        remoteUrl.weight = c.getInt('server.loadbalance.weight', RemoteUrl.DEFAULT_WEIGHT)

        addDefaultHandler()

        Runtime.addShutdownHook {
            stop()
        }
    }

    void stop() {
        if (metricsServer) {
            metricsServer.stop()
            log.info('stop metric server')
            metricsServer = null
        }
        if (registry) {
            registry.shutdown()
            registry = null
        }
        StatsHolder.instance.stop()
        if (workerGroup) {
            workerGroup.shutdownGracefully()
            log.info('worker group shutdown...')
            workerGroup = null
        }
        if (bossGroup) {
            bossGroup.shutdownGracefully()
            log.info('boss group shutdown...')
            bossGroup = null
        }
        if (handlerGroup) {
            handlerGroup.shutdownGracefully()
            log.info('handler group shutdown...')
            handlerGroup = null
        }
    }

    void start() {
        if (c.isOn('server.metric.export')) {
            DefaultExports.initialize()
            int metricServerPort = c.getInt('server.metric.listen.port', 8878)
            metricsServer = new HTTPServer(remoteUrl.host, metricServerPort)
            log.info('start metric server {}:{}', remoteUrl.host, metricServerPort)
        }

        registry = SpiSupport.getRegistry(c)
        registry.init(c)

        StatsHolder.instance.init(remoteUrl)

        def cpuNumber = Runtime.getRuntime().availableProcessors()
        int handleGroupThreadNumber = c.getInt('server.handle.group.thread.number', cpuNumber * 10)
        def threadFactory = new ThreadFactoryBuilder()
                .setNameFormat('service-handler-group' + "-%d")
                .setDaemon(false).build()
        handlerGroup = new DefaultEventExecutorGroup(handleGroupThreadNumber, threadFactory)

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
                                    .addLast(handlerGroup, new RpcHandler(c, remoteUrl))
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)

            registry.register(remoteUrl)

            log.info('server ready to start {}', remoteUrl)
            def future = bootstrap.bind(remoteUrl.host, remoteUrl.port).sync()
            future.channel().closeFuture().sync()
        } catch (InterruptedException e) {
            log.error('server start interrupted error', e)
        } finally {
            stop()
        }
    }

    private void addDefaultHandler() {
        def h = ChainHandler.instance
        h.remoteUrl = remoteUrl
        // add method invoke handler
        h.context(remoteUrl.context).
                get(ProxyCreator.HANDLER_URI, new MethodInvokeHandler(remoteUrl))

        // add data get for management
        h.group('/remote') {
            h.get('/overview') { req ->
                def uriList = h.list.collect {
                    it.name()
                }
                def methodList = DefaultProvider.instance.allMethods().collect {
                    [clazz: it.clazz, method: it.method, paramTypes: it.paramTypes.collect { Class type ->
                        type.name
                    }]
                }
                def r = [clientChannelInfo: RpcHandler.clientChannelInfoHolder,
                         uriList          : uriList,
                         methodList       : methodList]
                Resp.one(r)
            }
        }
    }
}
