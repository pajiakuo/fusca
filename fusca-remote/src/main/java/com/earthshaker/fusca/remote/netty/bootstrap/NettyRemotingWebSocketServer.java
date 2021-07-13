package com.earthshaker.fusca.remote.netty.bootstrap;

import cn.hutool.core.lang.Pair;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.earthshaker.fusca.remote.RemotingSysResponseCode;
import com.earthshaker.fusca.remote.ResponseCode;
import com.earthshaker.fusca.remote.constanst.ResponseTypeEnum;
import com.earthshaker.fusca.remote.netty.common.MessagePack;
import com.earthshaker.fusca.remote.netty.common.head.WebSocketCommonResponseHeadPackHead;
import com.earthshaker.fusca.remote.netty.util.NettyChannelUtil;
import com.earthshaker.fusca.remote.netty.util.RemotingHelper;
import com.google.common.base.Strings;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/5 5:58 下午
 */
public class NettyRemotingWebSocketServer extends NettyRemotingAbstract implements RemotingService {

    private static Log log = LogFactory.getCurrentLogFactory().getLog(NettyRemotingServer.class);

    private final ServerBootstrap serverBootstrap;
    private final EventLoopGroup eventLoopGroupSelector;
    private final EventLoopGroup eventLoopGroupBoss;
    private final NettyServerConfig nettyServerConfig;
    private final ChannelEventOperatorListener channelEventOperatorListener;

    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    private final ExecutorService publicExecutor;

    private NettyWSConnectManageHandler nettyWSConnectManageHandler;

    private NettyWsServerHandler nettyWsServerHandler;

    private final Timer timer = new Timer("WSServerHouseKeepingService", true);

    private int port = 0;

    public NettyRemotingWebSocketServer(final NettyServerConfig nettyServerConfig,
                               final ChannelEventOperatorListener channelEventOperatorListener) {
        super(nettyServerConfig.getServerOnewaySemaphoreValue(), nettyServerConfig.getServerAsyncSemaphoreValue());

        this.serverBootstrap = new ServerBootstrap();
        this.nettyServerConfig = nettyServerConfig;
        this.channelEventOperatorListener = channelEventOperatorListener;

        int publicThreadNums = nettyServerConfig.getServerCallbackExecutorThreads();
        if (publicThreadNums <= 0) {
            publicThreadNums = 4;
        }

        this.publicExecutor = Executors.newFixedThreadPool(publicThreadNums, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyWSServerPublicExecutor_" + this.threadIndex.incrementAndGet());
            }
        });

        if (useEpoll()) {
            this.eventLoopGroupBoss = new EpollEventLoopGroup(1, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyEPOLLBoss_%d", this.threadIndex.incrementAndGet()));
                }
            });

            this.eventLoopGroupSelector = new EpollEventLoopGroup(nettyServerConfig.getServerSelectorThreads(), new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int threadTotal = nettyServerConfig.getServerSelectorThreads();

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyWSServerEPOLLSelector_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
                }
            });
        } else {
            this.eventLoopGroupBoss = new NioEventLoopGroup(1, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyNIOBoss_%d", this.threadIndex.incrementAndGet()));
                }
            });

            this.eventLoopGroupSelector = new NioEventLoopGroup(nettyServerConfig.getServerSelectorThreads(), new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int threadTotal = nettyServerConfig.getServerSelectorThreads();

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyWSServerNIOSelector_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
                }
            });
        }

        this.timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                try {
                    NettyRemotingWebSocketServer.this.scanResponseTable();
                } catch (Throwable e) {
                    log.error("scanResponseTable exception", e);
                }
            }
        }, 1000 * 3, 1000);    }

    private boolean useEpoll() {
        return NettyChannelUtil.isLinuxPlatform()
                && nettyServerConfig.isUseEpollNativeSelector()
                && Epoll.isAvailable();
    }


    @Override
    public ChannelEventOperatorListener getChannelEventOperatorListener() {
        return this.channelEventOperatorListener;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
         return this.publicExecutor;
    }

    @Override
    public void start() {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(
                nettyServerConfig.getServerWorkerThreads(),
                new ThreadFactory() {

                    private AtomicInteger threadIndex = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "NettyWSServerCodecThread_" + this.threadIndex.incrementAndGet());
                    }
                });

        nettyWSConnectManageHandler = new NettyWSConnectManageHandler();

        ServerBootstrap childHandler =
                this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
                        .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        .option(ChannelOption.SO_KEEPALIVE, false)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getServerSocketSndBufSize())
                        .childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getServerSocketRcvBufSize())
                        .localAddress(new InetSocketAddress(this.nettyServerConfig.getListenWsPort()))
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch)
                                    throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast("http-codec",
                                        new HttpServerCodec());
                                pipeline.addLast("aggregator",
                                        new HttpObjectAggregator(65536));
                                ch.pipeline().addLast("http-chunked",
                                        new ChunkedWriteHandler());
                                ch.pipeline().addLast("idleState-handler",new IdleStateHandler(0, 0, 120));
                                ch.pipeline().addLast("ws-connect-handler",nettyWSConnectManageHandler);
                                pipeline.addLast("ws-message-handler",
                                        new NettyWsServerHandler());
                            }
                        });

        if (nettyServerConfig.isServerPooledByteBufAllocatorEnable()) {
            childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }

        try {
            ChannelFuture sync = this.serverBootstrap.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) sync.channel().localAddress();
            this.port = addr.getPort();
        } catch (InterruptedException e1) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e1);
        }

        if (this.channelEventOperatorListener != null) {
            this.nettyEventExecutor.start();
        }

        this.timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                try {
                    NettyRemotingWebSocketServer.this.scanResponseTable();
                } catch (Throwable e) {
                    log.error("scanResponseTable exception", e);
                }
            }
        }, 1000 * 3, 1000);
    }

    @Override
    public void shutdown() {
        try {
            if (this.timer != null) {
                this.timer.cancel();
            }

            this.eventLoopGroupBoss.shutdownGracefully();

            this.eventLoopGroupSelector.shutdownGracefully();

            if (this.nettyEventExecutor != null) {
                this.nettyEventExecutor.shutdown();
            }

            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            log.error("NettyRemotingServer shutdown exception, ", e);
        }

        if (this.publicExecutor != null) {
            try {
                this.publicExecutor.shutdown();
            } catch (Exception e) {
                log.error("NettyRemotingWsServer shutdown exception, ", e);
            }
        }
    }

    public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {
        ExecutorService executorThis = executor;
        if (null == executor) {
            executorThis = this.publicExecutor;
        }

        Pair<NettyRequestProcessor, ExecutorService> pair = new Pair<NettyRequestProcessor, ExecutorService>(processor, executorThis);
        this.processorTable.put(requestCode, pair);
    }

    public void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor) {
        this.defaultRequestProcessor = new Pair<NettyRequestProcessor, ExecutorService>(processor, executor);
    }

    class NettyWsServerHandler extends SimpleChannelInboundHandler<Object> {
        private WebSocketServerHandshaker handshaker;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            //传统的http接入
            if(msg instanceof FullHttpRequest){
                handleHttpRequest(ctx,(FullHttpRequest) msg);
            }
            //webSocket接入
            else if(msg instanceof WebSocketFrame){
                handleWebsocketFrame(ctx,(WebSocketFrame) msg);
            }
        }
        private void handleHttpRequest(ChannelHandlerContext ctx,FullHttpRequest fullHttpRequest){
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                    "ws://127.0.0.1:9999/websocket", null, false);
            handshaker = wsFactory.newHandshaker(fullHttpRequest);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory
                        .sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), fullHttpRequest);
            }
        }

        private void handleWebsocketFrame(ChannelHandlerContext ctx,WebSocketFrame webSocketFrame){
            if (webSocketFrame instanceof CloseWebSocketFrame){
                handshaker.close(ctx.channel(),(CloseWebSocketFrame) webSocketFrame.retain());
                return;
            }
            if(webSocketFrame instanceof PingWebSocketFrame){
                ctx.channel().write(new PongWebSocketFrame(webSocketFrame.content().retain()));
                return;
            }
            if (webSocketFrame instanceof TextWebSocketFrame){
                TextWebSocketFrame frame = (TextWebSocketFrame) webSocketFrame;
                String content = frame.text();
                try {
                    if (Strings.isNullOrEmpty(content)){
                        log.warn("前端传输数据不存在{}",RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
                        if (ctx.channel().isActive()&&ctx.channel().isWritable()){
                            WebSocketCommonResponseHeadPackHead packHead = new WebSocketCommonResponseHeadPackHead();
                            packHead.setResponseTimestamp(System.currentTimeMillis());
                            RemotingHelper.writeChannelResponse(ctx,MessagePack.createResponseCommand(ResponseCode.NO_MESSAGE,packHead,"请求参数数据为空",ResponseTypeEnum.TEXT_WEB_SOCKET_FRAME.getResponseType()));
                            return;
                        }else {
                            //todo 关闭关系
                            log.warn("前端传输通道已关闭{}",RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
                        }
                    }
                }catch (Exception e){
                    WebSocketCommonResponseHeadPackHead packHead = new WebSocketCommonResponseHeadPackHead();
                    packHead.setResponseTimestamp(System.currentTimeMillis());
                    log.error("系统异常{} client 地址{}",content,RemotingHelper.parseChannelRemoteAddr(ctx.channel()),e);
                    RemotingHelper.writeChannelResponse(ctx,MessagePack.createResponseCommand(RemotingSysResponseCode.SYSTEM_ERROR,packHead,"系统异常",ResponseTypeEnum.TEXT_WEB_SOCKET_FRAME.getResponseType()));
                    return;
                }
                MessagePack message = null;
                try {
                    message = MessagePack.parseRequestCommand(content);
                    message.setResponseType(ResponseTypeEnum.TEXT_WEB_SOCKET_FRAME.getResponseType());
                    if (message==null){
                        WebSocketCommonResponseHeadPackHead packHead = new WebSocketCommonResponseHeadPackHead();
                        packHead.setResponseTimestamp(System.currentTimeMillis());
                        RemotingHelper.writeChannelResponse(ctx,MessagePack.createResponseCommand(ResponseCode.MESSAGE_ILLEGAL,packHead,"解析请求数据格式异常",ResponseTypeEnum.TEXT_WEB_SOCKET_FRAME.getResponseType()));
                        return;
                    }
                    processMessageReceived(ctx, message);
                    return;
                }catch (Exception e){
                    log.error("处理请求异常{} client 地址{}",content,RemotingHelper.parseChannelRemoteAddr(ctx.channel()),e);
                    WebSocketCommonResponseHeadPackHead packHead = new WebSocketCommonResponseHeadPackHead();
                    packHead.setResponseTimestamp(System.currentTimeMillis());
                    RemotingHelper.writeChannelResponse(ctx,MessagePack.createResponseCommand(ResponseCode.MESSAGE_ILLEGAL,packHead,"服务处理请求异常",ResponseTypeEnum.TEXT_WEB_SOCKET_FRAME.getResponseType()));
                    return;
                }
            }

            //未知的处理类型
            WebSocketCommonResponseHeadPackHead packHead = new WebSocketCommonResponseHeadPackHead();
            RemotingHelper.writeChannelResponse(ctx,MessagePack.createResponseCommand(ResponseCode.MESSAGE_ILLEGAL,packHead,"未知的websocket类型",ResponseTypeEnum.TEXT_WEB_SOCKET_FRAME.getResponseType()));
            return;
        }
    }




    @ChannelHandler.Sharable
    class NettyWSConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelRegistered {}", remoteAddress);
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelUnregistered, the channel[{}]", remoteAddress);
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelActive, the channel[{}]", remoteAddress);
            super.channelActive(ctx);

            //todo 保存客户端的channel 连接
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelInactive, the channel[{}]", remoteAddress);
            super.channelInactive(ctx);

            //todo 保存客户端的channel 连接

        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                    log.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", remoteAddress);
                    NettyChannelUtil.closeChannel(ctx.channel());
                }
            }

            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            log.warn("NETTY SERVER PIPELINE: exceptionCaught {}", remoteAddress);
            log.warn("NETTY SERVER PIPELINE: exceptionCaught exception.", cause);

            NettyChannelUtil.closeChannel(ctx.channel());
        }
    }
}
