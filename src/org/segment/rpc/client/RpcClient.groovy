package org.segment.rpc.client

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.timeout.IdleStateHandler
import org.segment.rpc.client.loadbalance.LoadBalance
import org.segment.rpc.common.RpcConf
import org.segment.rpc.common.SpiSupport
import org.segment.rpc.server.codec.Decoder
import org.segment.rpc.server.codec.Encoder
import org.segment.rpc.server.codec.RpcMessage
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.registry.EventTrigger
import org.segment.rpc.server.registry.EventType
import org.segment.rpc.server.registry.Registry
import org.segment.rpc.server.registry.RemoteUrl
import org.segment.rpc.server.serialize.Compress
import org.segment.rpc.server.serialize.CompressFactory
import org.segment.rpc.server.serialize.Serializer
import org.segment.rpc.server.serialize.SerializerFactory

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@CompileStatic
@Slf4j
class RpcClient {
    private RpcConf c

    private EventLoopGroup eventLoopGroup
    private final Bootstrap bootstrap

    private Registry registry
    private LoadBalance loadBalance

    private ResponseFutureHolder responseFutureHolder

    private ChannelHolder channelHolder

    // client uuid
    private String uuid

    String getUuid() {
        return uuid
    }

    private volatile boolean isStopped = false

    void stop() {
        isStopped = true

        if (registry) {
            registry.shutdown()
            registry = null // if stop many times, log to many
        }
        if (loadBalance) {
            loadBalance.shutdown()
            loadBalance = null
        }
        if (channelHolder) {
            channelHolder.disconnectAll()
            channelHolder = null
        }
        if (eventLoopGroup) {
            eventLoopGroup.shutdownGracefully()
            log.info('event loop group shutdown...')
            eventLoopGroup = null
        }
        if (responseFutureHolder) {
            responseFutureHolder.stop()
            responseFutureHolder = null
        }
    }

    RpcClient(RpcConf conf = null) {
        this.c = conf ?: RpcConf.fromLoad()
        log.info c.toString()
        c.on('is.client.running')

        uuid = UUID.randomUUID().toString()

        channelHolder = new ChannelHolder()

        responseFutureHolder = new ResponseFutureHolder()
        responseFutureHolder.init(c)

        eventLoopGroup = new NioEventLoopGroup()
        bootstrap = new Bootstrap()

        Runtime.addShutdownHook {
            stop()
        }

        long writerIdleTimeSeconds = c.getInt('client.writer.idle.seconds', 10) as long
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        c.getInt('client.connect.timeout.millis', 2000))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new IdleStateHandler(0, writerIdleTimeSeconds, 0, TimeUnit.SECONDS))
                                .addLast(new Encoder())
                                .addLast(new Decoder())
                                .addLast(new RpcClientHandler(responseFutureHolder, channelHolder, registry))
                    }
                })

        loadBalance = SpiSupport.getLoadBalance()
        loadBalance.init(c)

        // registry init in the end, because need doConnect when get remote server list
        registry = SpiSupport.getRegistry(c)
        initEventHandler()
        registry.init(c)
    }

    synchronized Channel doConnect(RemoteUrl remoteUrl) {
        if (isStopped) {
            return null
        }

        def address = remoteUrl.address()
        def future = bootstrap.connect(address)

        def connectTimeoutMs = c.getInt('client.connect.timeout.millis', 2000)
        // sync
        def result = future.awaitUninterruptibly(connectTimeoutMs, TimeUnit.MILLISECONDS)
        def isSuccess = future.isSuccess()

        if (result && isSuccess) {
            log.info 'client connect ok, remote server: {}', remoteUrl
            return future.channel()
        }

        if (future.cause() != null) {
            future.cancel(true)
            log.error('client connect error, remote server: ' + remoteUrl.toString(), future.cause())
            return null
        }
        null
    }

    Resp sendSync(Req req, long timeoutMillis = 0) {
        // client config
        int timeoutMillisDefault = c.getInt('client.get.response.timeout.millis', 2000)
        long ms = timeoutMillis == 0 ? timeoutMillisDefault : timeoutMillis

        try {
            def future = send(req)
            return future.get(ms, TimeUnit.MILLISECONDS)
        } catch (TimeoutException e) {
            log.warn('send sync get response timeout, uri: {}, this retry time: {}, uuid: {}, timeout: {}ms',
                    req.uri, req.thisRetryTime, req.uuid, ms)
            responseFutureHolder.remove(req.uuid)

            if (req.needRetry) {
                log.warn 'send sync get response timeout, do retry, uri: {}, this retry time: {}, uuid: {}',
                        req.uri, req.thisRetryTime, req.uuid
                req.needRetry = false
                return sendSync(req, ms)
            } else {
                throw e
            }
        }
    }

    // only sync call for user
    private CompletableFuture<Resp> send(Req req) {
        if (isStopped) {
            throw new IllegalStateException('rpc client stopped')
        }

        def remoteUrl = loadBalance.select(registry.discover(req), req)
        if (remoteUrl == null) {
            log.warn('remote server not found, remote server: {}, uri: {}, this retry time: {}, uuid: {}',
                    remoteUrl, req.uri, req.thisRetryTime, req.uuid)
            throw new RemoteUrlDownException('server not found')
        }

        int clientRetries = remoteUrl.getInt('client.send.retries', 1)
        for (int i = req.thisRetryTime; i <= clientRetries; i++) {
            // for timeout retry
            req.needRetry = i < clientRetries
            try {
                return sendOnce(remoteUrl, req)
            } catch (RemoteUrlDownException e) {
                registry.unavailable(remoteUrl)
                // need change a remote url, reset retries
                req.thisRetryTime = 0
                req.needRetry = true

                int remoteDownRetryWaitMillis = c.getInt('client.remote.down.retry.wait.millis', 50)
                log.warn('remote server down, wait send retry another, remote server: {}, uri: {}, this retry time: {}, uuid: {}, wait: {}ms',
                        remoteUrl, req.uri, req.thisRetryTime, req.uuid, remoteDownRetryWaitMillis)
                Thread.sleep(remoteDownRetryWaitMillis)
                return send(req)
            } catch (RuntimeException e) {
                log.warn('send request error and server: {}, uri: {}, this retry time: {}, uuid: {}, message: {}',
                        remoteUrl, req.uri, req.thisRetryTime, req.uuid, e.message)
                if (!req.needRetry) {
                    throw e
                }
            } finally {
                req.thisRetryTime = i + 1
            }
        }

        log.warn 'why!! remote server: {}, uri: {}, this retry time: {}, uuid: {}',
                remoteUrl, req.uri, req.thisRetryTime, req.uuid
        throw new RuntimeException('send request error')
    }

    private CompletableFuture<Resp> sendOnce(RemoteUrl remoteUrl, Req req, Channel targetChannel = null) {
        def channel = targetChannel ?: channelHolder.getActive(remoteUrl)
        // when registry found new server list, do connect and add to channel holder already
        // if server crash down
        if (channel == null) {
            log.warn('channel not found, remove server: {}, uri: {}, this retry time: {}, uuid: {}',
                    remoteUrl, req.uri, req.thisRetryTime, req.uuid)
            // if server crash down, connect fail
            throw new RemoteUrlDownException('channel not found, remote server: ' + remoteUrl)
        }

        def msg = new RpcMessage()
        msg.data = req
        msg.messageType = RpcMessage.Type.REQ

        fillDefaultConfigItem(msg)
        // reset compress type if need
        if (req.compressType != null) {
            msg.compressType = req.compressType
        }
        // reset serialize type if need
        if (req.serializeType != null) {
            msg.serializeType = req.serializeType
        }

        msg.dataToBytes()

        // registry config
        int timeoutMillis = remoteUrl.getInt('client.get.response.timeout.millis', 2000)
        int requestTimeoutMillis = remoteUrl.getInt('client.request.timeout.millis', 200)

        def resultFuture = new CompletableFuture<Resp>()
        responseFutureHolder.put(req.uuid, resultFuture, timeoutMillis)

        def writeFuture = channel.writeAndFlush(msg)
        boolean result = writeFuture.awaitUninterruptibly(requestTimeoutMillis, TimeUnit.MILLISECONDS)
        if (result && writeFuture.isSuccess()) {
            return resultFuture
        }

        writeFuture.cancel(true)
        responseFutureHolder.remove(req.uuid)

        // if this channel is down, use another active channel
        def channelAnother = channelHolder.getActiveExcludeOne(remoteUrl, channel)
        if (channelAnother) {
            // try again
            log.warn('send once timeout, do retry, remote server: {}, uri: {}, this retry time: {}, uuid: {}',
                    remoteUrl, req.uri, req.thisRetryTime, req.uuid)
            return sendOnce(remoteUrl, req, channelAnother)
        }

        if (writeFuture.cause()) {
            throw new RemoteUrlDownException('channel is not writable, remote server: ' + remoteUrl + ', ex: '
                    + writeFuture.cause().class.name)
        } else {
            // timeout
            throw new RemoteUrlDownException('channel write timeout, remote server: ' + remoteUrl)
        }
    }

    void fillDefaultConfigItem(RpcMessage msg) {
        if (c.isOn('client.send.request.use.gzip')) {
            msg.compressType = Compress.Type.GZIP
        } else if (c.isOn('client.send.request.use.lz4')) {
            msg.compressType = Compress.Type.LZ4
        } else {
            msg.compressType = CompressFactory.hasVendorCompress ?
                    Compress.Type.CUSTOM : Compress.Type.NONE
        }

        if (c.isOn('client.send.request.serialize.type.use.kyro')) {
            msg.serializeType = Serializer.Type.KYRO
        } else if (c.isOn('client.send.request.serialize.type.use.hessian')) {
            msg.serializeType = Serializer.Type.HESSIAN
        } else {
            msg.serializeType = SerializerFactory.hasVendorSerializer ? Serializer.Type.CUSTOM : Serializer.Type.KYRO
        }
    }

    private void initEventHandler() {
        registry.addEvent(new EventTrigger() {
            @Override
            EventType type() {
                EventType.NEW_ADDED
            }

            @Override
            void handle(RemoteUrl remoteUrl) {
                if (remoteUrl.weight == 0) {
                    log.warn 'server weight is 0, need not connect, remote server: {}', remoteUrl
                    int activeNumber = channelHolder.getActiveNumber(remoteUrl)
                    if (activeNumber > 0) {
                        log.warn 'server weight is 0, but have active channel, disconnect, remote server: {}', remoteUrl
                        registry.unavailable(remoteUrl)
                        channelHolder.disconnect(remoteUrl)
                    }
                    return
                }

                int needCreateChannelNumberRemote = remoteUrl.getInt(RpcConf.CLIENT_CHANNEL_NUMBER_PER_SERVER, 2)
                int needCreateChannelNumberClient = c.getInt(RpcConf.CLIENT_CHANNEL_NUMBER_PER_SERVER, 2)
                int needCreateChannelNumber = Math.min(needCreateChannelNumberRemote, needCreateChannelNumberClient)

                int activeNumber = channelHolder.getActiveNumber(remoteUrl)
                if (activeNumber >= needCreateChannelNumber) {
                    log.warn 'already have active channel, remote server: {}, active number {}', remoteUrl, activeNumber
                    return
                }

                log.warn 'need connect channel, remote server: {}, need create number: {}', remoteUrl, needCreateChannelNumber - activeNumber
                for (int i = activeNumber; i < needCreateChannelNumber; i++) {
                    def newOne = RpcClient.this.doConnect(remoteUrl)
                    if (newOne) {
                        channelHolder.add(remoteUrl, newOne)
                    } else {
                        log.warn 'do connect failed, remote server: {}', remoteUrl
                    }
                }
            }
        })
    }
}
