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
import org.segment.rpc.client.loadbalance.LoadBalance
import org.segment.rpc.common.RpcConf
import org.segment.rpc.common.SpiSupport
import org.segment.rpc.server.codec.Decoder
import org.segment.rpc.server.codec.Encoder
import org.segment.rpc.server.codec.RpcMessage
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.registry.*
import org.segment.rpc.server.serialize.Serializer

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

    private String uuid

    String getUuid() {
        return uuid
    }

    private volatile boolean isStopped = false

    void stop() {
        isStopped = true

        if (registry) {
            registry.shutdown()
            registry = null
        }
        ChannelHolder.instance.disconnect()
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

        initEventHandler()

        uuid = UUID.randomUUID().toString()

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
                                .addLast(new RpcClientHandler(responseFutureHolder))
                    }
                })

        loadBalance = SpiSupport.getLoadBalance()
        loadBalance.init()
        // registry init in the end, because need doConnect when get remote server list
        registry = SpiSupport.getRegistry(c)
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
            log.info 'client connect to remote server ok {}', address.toString()
            return future.channel()
        }

        if (future.cause() != null) {
            future.cancel(false)
            log.error('client connect to remote server error ' + address.toString(), future.cause())
            return null
        }
        null
    }

    private final String NEED_RETRY_HEADER = 'needRetry'

    Resp sendSync(Req req, long timeoutMillis = 0) {
        // client config
        int timeoutMillisDefault = c.getInt('client.get.response.timeout.millis', 2000)
        long ms = timeoutMillis == 0 ? timeoutMillisDefault : timeoutMillis

        def future = send(req)
        try {
            return future.get(ms, TimeUnit.MILLISECONDS)
        } catch (TimeoutException e) {
            log.warn('send request timeout - uri {}, retries {}, timeoutMs {}', req.uri, req.retries, ms)
            responseFutureHolder.complete(new Resp(status: Resp.Status.INTERNAL_EX, uuid: req.uuid))

            boolean needRetry = Boolean.valueOf(req.header(NEED_RETRY_HEADER))
            if (needRetry) {
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
            throw new IllegalStateException('no remote server found while request uri context - ' + req.context())
        }

        int retries = remoteUrl.getInt('client.send.retries', 0)
        for (int i = req.retries; i <= retries; i++) {
            // for timeout retry
            req.retries = i + 1
            boolean needRetry = i < retries
            req.header(NEED_RETRY_HEADER, needRetry.toString())
            try {
                return sendOnce(remoteUrl, req)
            } catch (RuntimeException e) {
                log.warn('send request error - {}, retries: {}, message {}', remoteUrl, i, e.message)
                if (!needRetry) {
                    throw e
                }
            }
        }

        log.warn('why??? server: {} uri: {}', remoteUrl, req.uri)
        throw new RuntimeException('send request error')
    }

    private CompletableFuture<Resp> sendOnce(RemoteUrl remoteUrl, Req req) {
        def channel = ChannelHolder.instance.get(remoteUrl)
        // will never happen
        // because when registry found new server list, do connect and add to channel holder already
        if (channel == null) {
            def newOne = doConnect(remoteUrl)
            if (newOne) {
                ChannelHolder.instance.add(remoteUrl, newOne)
                channel = newOne
            } else {
                throw new IllegalStateException('channel not found - ' + remoteUrl)
            }
        }

        def msg = new RpcMessage()
        msg.data = req
        msg.messageType = RpcMessage.MessageType.REQ
        initRpcMessage(msg)

        // registry config
        int timeoutMillis = remoteUrl.getInt('client.get.response.timeout.millis', 2000)
        def resultFuture = new CompletableFuture<Resp>()
        responseFutureHolder.put(req.uuid, resultFuture, timeoutMillis)

        channel.writeAndFlush(msg).addListener({ ChannelFuture future ->
            if (!future.isSuccess()) {
                log.error 'write and flush request error - ' + remoteUrl, future.cause()
                resultFuture.completeExceptionally(future.cause())
                future.channel().close()
            } else {
                if (log.isDebugEnabled()) {
                    // data may be too long, to string will cost too much time
                    log.debug('write and flush request ok {}', msg.data)
                }
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

    private void initEventHandler() {
        EventHandler.instance.add(new EventTrigger() {
            @Override
            EventType type() {
                EventType.NEW_ADDED
            }

            @Override
            void handle(RemoteUrl remoteUrl) {
                // use local or remote ?
//                remoteUrl.extend(c.params, false)
                int needCreateChannelNumber = remoteUrl.getInt(RpcConf.CLIENT_CHANNEL_NUMBER_PER_SERVER, 2)
                for (int i = 0; i < needCreateChannelNumber; i++) {
                    def newOne = RpcClient.this.doConnect(remoteUrl)
                    if (newOne) {
                        ChannelHolder.instance.add(remoteUrl, newOne)
                    }
                }
            }
        })
    }
}
