package com.earthshaker.fusca.remote.netty.bootstrap;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.earthshaker.fusca.remote.RemotingSysResponseCode;
import com.earthshaker.fusca.remote.RequestCode;
import com.earthshaker.fusca.remote.exception.RemotingConnectException;
import com.earthshaker.fusca.remote.exception.RemotingSendRequestException;
import com.earthshaker.fusca.remote.exception.RemotingTimeoutException;
import com.earthshaker.fusca.remote.exception.RemotingTooMuchRequestException;
import com.earthshaker.fusca.remote.netty.common.InvokeCallback;
import com.earthshaker.fusca.remote.netty.common.MessagePack;
import com.earthshaker.fusca.remote.netty.common.ResponseFuture;
import com.earthshaker.fusca.remote.netty.common.head.ClientManagerRequestPackHead;
import com.earthshaker.fusca.remote.netty.processor.ClientManagerProcessor;
import com.earthshaker.fusca.remote.netty.processor.SendMessageProcessor;
import com.earthshaker.fusca.remote.netty.processor.SideSynchronizationProcessor;
import com.earthshaker.fusca.remote.netty.processor.ws.WsClientMangerProcessor;
import com.earthshaker.fusca.remote.netty.processor.ws.WsSendMessageProcessor;
import com.earthshaker.fusca.remote.netty.util.NettyChannelUtil;
import com.earthshaker.fusca.remote.netty.util.RemotingHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/7 10:10 上午
 */
public class NettyBootStrap {

    private static Log log = LogFactory.getCurrentLogFactory().getLog(NettyBootStrap.class);


    private NettyRemotingWebSocketServer nettyRemotingWebSocketServer;

    private NettyRemotingClient nettyRemotingClient;

    private NettyRemotingServer nettyRemotingServer;

    private final NettyServerConfig nettyServerConfig;
    private final NettyClientConfig nettyClientConfig;

    private static AtomicInteger requestId = new AtomicInteger(0);

    /**
     * client 连接集合服务列表
     */
    private final AtomicReference<List<String>> configurationAddrList = new AtomicReference<List<String>>();

    /**
     * 未建立的server地址
     */
    private final AtomicReference<List<String>> failAddrList = new AtomicReference<List<String>>();

    /**
     * 成功建立连接的地址 心跳的建立
     */
    private final AtomicReference<List<String>> successAddrList = new AtomicReference<List<String>>();

    /**
     * client 连接锁
     */
    private final Lock lockHeartbeat = new ReentrantLock();

    private final String clientId;

    /**
     *  key 代表group
     *  二级 客户/游客/op sessionId 与 ClientChannelInfo集合 关系
     */
    private  ConcurrentHashMap<String,ConcurrentHashMap<String,List<ClientChannelInfo>>> groupClientChannelMap = new ConcurrentHashMap<>();


    private Map<String,String> customerRelationMap = Maps.newHashMap();
    private Map<String,String> opRelationMap = Maps.newHashMap();

    /**
     * 新跳上传地址
     * key 是组的意思 channel 使用时需要判断状态
     */
    public final ConcurrentHashMap<String, ConcurrentHashMap<Channel, ClientChannelInfo>> clientChannelManager = new ConcurrentHashMap<>();

    public final static String INSIDE_GROUP = "INSIDE_GROUP";

    public final static String WS_OUTSIDE_GROUP = "OUTSIDE_GROUP";

    /**
     * channel
     */
    private ScheduledExecutorService scheduledExecutorService = Executors
            .newSingleThreadScheduledExecutor();

    private final ScheduledExecutorService scheduledHeartbeatExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "NettyBootStrap Heartbeat ScheduledThread");
        }
    });

    private final ChannelEventOperatorListener channelEventOperatorListener0;
    private final ChannelEventOperatorListener channelEventOperatorListener1;
    private final ChannelEventOperatorListener channelEventOperatorListener2;

    private final ExecutorService sendMessageProcessorExecutor;
    private final ExecutorService sideSynchronizationProcessorExecutor;


    /**
     *
     * @param nettyServerConfig
     * @param nettyClientConfig
     * @param channelEventOperatorListener0 client
     * @param channelEventOperatorListener1 ws
     * @param channelEventOperatorListener2 server
     */
    public NettyBootStrap(NettyServerConfig nettyServerConfig,
                          NettyClientConfig nettyClientConfig ,
                          ChannelEventOperatorListener channelEventOperatorListener0,
                          ChannelEventOperatorListener channelEventOperatorListener1,
                          ChannelEventOperatorListener channelEventOperatorListener2
    ) {
        this.nettyClientConfig = nettyClientConfig;
        this.nettyServerConfig = nettyServerConfig;
        this.channelEventOperatorListener0 = channelEventOperatorListener0;
        this.channelEventOperatorListener1 = channelEventOperatorListener1;
        this.channelEventOperatorListener2 = channelEventOperatorListener2;
        this.nettyRemotingClient = new NettyRemotingClient(nettyClientConfig,channelEventOperatorListener0);
        this.nettyRemotingWebSocketServer = new NettyRemotingWebSocketServer(nettyServerConfig,channelEventOperatorListener1);
        this.nettyRemotingServer = new NettyRemotingServer(nettyServerConfig,channelEventOperatorListener2);
        this.clientId = UUID.fastUUID().toString();

        this.sendMessageProcessorExecutor = Executors.newFixedThreadPool(100, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "SendMessageProcessorExecutor_" + this.threadIndex.incrementAndGet());
            }
        });
        this.sideSynchronizationProcessorExecutor  =  Executors.newFixedThreadPool(100, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "SideSynchronizationProcessorExecutor_" + this.threadIndex.incrementAndGet());
            }
        });

        customerRelationMap.put("12","67");
        customerRelationMap.put("123xsdsd","67");
        opRelationMap.put("67","12");
        opRelationMap.put("67","123xsdsd");
    }

    public Map<String, String> getCustomerRelationMap() {
        return customerRelationMap;
    }

    public Map<String, String> getOpRelationMap() {
        return opRelationMap;
    }



    /**
     * 管理
     */
    public ConcurrentHashMap<Channel, ClientChannelInfo> managerClientChannelManager(String groupName){
       return clientChannelManager.get(groupName);
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Channel, ClientChannelInfo>> getClientChannelManager(){
        return clientChannelManager;
    }

    public NettyRemotingClient getNettyRemotingClient() {
        return nettyRemotingClient;
    }


    /**
     * 维护
     * @return
     */
    public Integer getIncrement (){
       return requestId.getAndIncrement();
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, List<ClientChannelInfo>>> getGroupClientChannelMap() {
        return groupClientChannelMap;
    }


    /**
     *
     */
    public void registerClientConnectAddress(List<String> addressList){
        configurationAddrList.set(addressList);
        updateNameServerAddressList(addressList);
    }


    /**
     * 启动Server Client WS-Server
     * 启动 channle 活跃检查
     * 启动client 集群 定时心跳
     */
    public void start(){
        log.info("启动服务 本地client 的clientId{}",clientId);
        registerClientProcessor();
        registerServerProcessor();
        registerWsServerProcessor();

        nettyRemotingServer.start();
        nettyRemotingClient.start();
        nettyRemotingWebSocketServer.start();

        this.scheduledHeartbeatExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    sendHeartbeatToAllServer();
                } catch (Exception e) {
                    log.error("ScheduledTask sendHeartbeatToAllServer exception", e);
                }
            }
        }, 1000, 1000*30, TimeUnit.MILLISECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    scanNotActiveChannel();
                } catch (Throwable e) {
                    log.error("Error occurred when scan not active client channels.", e);
                }
            }
        }, 1000 * 10, 1000 * 10, TimeUnit.MILLISECONDS);

        //
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            private volatile boolean hasShutdown = false;
            private AtomicInteger shutdownTimes = new AtomicInteger(0);

            @Override
            public void run() {
                synchronized (this) {
                    log.info("Shutdown hook was invoked, {}", this.shutdownTimes.incrementAndGet());
                    if (!this.hasShutdown) {
                        this.hasShutdown = true;
                        long beginTime = System.currentTimeMillis();
                        shutdown();
                        long consumingTimeTotal = System.currentTimeMillis() - beginTime;
                        log.info("Shutdown hook over, consuming total time(ms): {}", consumingTimeTotal);
                    }
                }
            }
        }, "ShutdownHook"));
    }


    /**
     *
     */
    public void shutdown(){
        scheduledExecutorService.shutdown();
        scheduledHeartbeatExecutorService.shutdown();
        if (this.sendMessageProcessorExecutor != null) {
            this.sendMessageProcessorExecutor.shutdown();
        }
        //通知下线要在client关闭前通知已经成功的连接 该客户端做下线
        unRegisterClient();
        //关闭 server连接的客户端/ws-server连接的客户端
        closeClientChannelManagerChannel();
        nettyRemotingWebSocketServer.shutdown();
        nettyRemotingServer.shutdown();
        nettyRemotingClient.shutdown();

        failAddrList.set(configurationAddrList.get());
    }

    /**
     * 更新client端的可连接地址
     * @param addrs
     */
    public void updateNameServerAddressList (List<String> addrs){
        nettyRemotingClient.updateNameServerAddressList(addrs);
    }

    public String getClientId() {
        return clientId;
    }

    private void registerClientProcessor(){
       // nettyRemotingClient.registerProcessor(RequestCode.HEART_BEAT,);

    }

    private void registerServerProcessor(){

        SendMessageProcessor sendMessageProcessor = new SendMessageProcessor(this);
        /**
         * server 接收到消息
         */
        nettyRemotingServer.registerProcessor(RequestCode.SEND_MESSAGE, sendMessageProcessor, sendMessageProcessorExecutor);

        ClientManagerProcessor clientManagerProcessor = new ClientManagerProcessor(this);
        nettyRemotingServer.registerProcessor(RequestCode.UNREGISTER_CLIENT,clientManagerProcessor , sendMessageProcessorExecutor);
        nettyRemotingServer.registerProcessor(RequestCode.HEART_BEAT,clientManagerProcessor , sendMessageProcessorExecutor);

        //多端同步
        SideSynchronizationProcessor sideSynchronizationProcessor = new SideSynchronizationProcessor(this);
        nettyRemotingServer.registerProcessor(RequestCode.SIDE_SYNC_CLIENT,sideSynchronizationProcessor,sideSynchronizationProcessorExecutor);

    }

    private void registerWsServerProcessor(){

        WsSendMessageProcessor sendMessageProcessor = new WsSendMessageProcessor(this);
        /**
         * server 接收到消息
         */
        nettyRemotingWebSocketServer.registerProcessor(RequestCode.SEND_MESSAGE_WS, sendMessageProcessor, sendMessageProcessorExecutor);

        WsClientMangerProcessor clientManagerProcessor = new WsClientMangerProcessor(this);
        nettyRemotingWebSocketServer.registerProcessor(RequestCode.UNREGISTER_CLIENT_WS,clientManagerProcessor , sendMessageProcessorExecutor);
        nettyRemotingWebSocketServer.registerProcessor(RequestCode.HEART_BEAT_WS,clientManagerProcessor , sendMessageProcessorExecutor);

    }

    public AtomicReference<List<String>> getSuccessAddrList() {
        return successAddrList;
    }

    /**
     * 发送给所有server心跳
     */
    public void sendHeartbeatToAllServer(){
        if (lockHeartbeat.tryLock()){
            try {
                if (CollectionUtil.isNotEmpty(configurationAddrList.get())){
                    ClientManagerRequestPackHead clientManagerRequestPackHead = new ClientManagerRequestPackHead();
                    clientManagerRequestPackHead.setClientID(clientId);
                    MessagePack messagePack = MessagePack.createSyncRequestCommand(RequestCode.HEART_BEAT,clientManagerRequestPackHead);
                    configurationAddrList.get().forEach(address ->{
                        List<String> successAddress = successAddrList.get();
                        try {
                            MessagePack messagePack1 = nettyRemotingClient.invokeSync(address,messagePack,3000);
                            if(RemotingSysResponseCode.SUCCESS == messagePack1.getCode()){
                               if (CollectionUtil.isNotEmpty(successAddress)){
                                   if (!successAddress.contains(address)){
                                       successAddress.add(address);
                                       successAddrList.set(successAddress);
                                   }
                               }else {
                                   successAddress = Lists.newArrayList();
                                   successAddress.add(address);
                                   successAddrList.set(successAddress);
                               }
                            }
                        }catch (Exception e){
                            if (CollectionUtil.isNotEmpty(successAddress)){
                                if (successAddress.contains(address)){
                                    successAddress.remove(address);
                                    successAddrList.set(successAddress);
                                }
                            }
                            log.error("send HeartbeatToAllServer exception address {}",address,e);
                        }
                    });
                }else {
                    log.warn("no possible configurationAddrList connect");
                }
            }catch (Exception e){
                log.error("send HeartbeatToAllServer exception",e);
            }finally {
                lockHeartbeat.unlock();
            }
        }else {
            log.warn("lock heartBeat, but failed. [{}]");
        }
    }

    /**
     *  client 下线集群通知给server
     */
    private void unRegisterClient(){
        try {
            if (this.lockHeartbeat.tryLock(3000, TimeUnit.MILLISECONDS)) {
                try {
                    List<String> successAddress = successAddrList.get();
                    if(CollectionUtil.isNotEmpty(successAddress)){
                        for (String address:successAddress){
                            ClientManagerRequestPackHead clientManagerRequestPackHead = new ClientManagerRequestPackHead();
                            clientManagerRequestPackHead.setClientID(clientId);
                            MessagePack messagePack = MessagePack.createSyncRequestCommand(RequestCode.UNREGISTER_CLIENT,clientManagerRequestPackHead);
                            nettyRemotingClient.invokeSync(address,messagePack,3000);
                        }
                    }

                } catch (Exception e) {
                    log.error("unregisterClient exception", e);
                } finally {
                    this.lockHeartbeat.unlock();
                }
            } else {
                log.warn("lock heartBeat, but failed. [{}]", this.clientId);
            }
        } catch (InterruptedException e) {
            log.warn("unregisterClientWithLock exception", e);
        }

    }

    public void scanNotActiveChannel() {
        //扫码 三者心跳超时的channel
        for (final Map.Entry<String, ConcurrentHashMap<Channel, ClientChannelInfo>> entry : this.clientChannelManager
                .entrySet()) {
            final String group = entry.getKey();
            final ConcurrentHashMap<Channel, ClientChannelInfo> chlMap = entry.getValue();
            Iterator<Map.Entry<Channel, ClientChannelInfo>> it = chlMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Channel, ClientChannelInfo> item = it.next();
                // final Integer id = item.getKey();
                final ClientChannelInfo info = item.getValue();

                long diff = System.currentTimeMillis() - info.getLastUpdateTimestamp();
                if (diff > nettyClientConfig.getClientChannelMaxIdleTimeSeconds()*1000) {
                    it.remove();
                   // clientChannelTable.remove(info.getClientId());
                    log.warn(
                            "SCAN: remove expired channel[{}] from ProducerManager groupChannelTable, producer group name: {}",
                            RemotingHelper.parseChannelRemoteAddr(info.getChannel()), group);
                    NettyChannelUtil.closeChannel(info.getChannel());
                }
            }
        }

    }

    public void closeClientChannelManagerChannel() {
        for (final Map.Entry<String, ConcurrentHashMap<Channel, ClientChannelInfo>> entry : this.clientChannelManager
                .entrySet()) {
            final ConcurrentHashMap<Channel, ClientChannelInfo> chlMap = entry.getValue();
            Iterator<Map.Entry<Channel, ClientChannelInfo>> it = chlMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Channel, ClientChannelInfo> item = it.next();
                final ClientChannelInfo info = item.getValue();
                NettyChannelUtil.closeChannel(info.getChannel());
            }
        }

    }

    public String groupName(Integer source){
        switch (source){
            case  1:
                return "customer";
            case 2:
                return "op_user";
            case 3:
                return "tourist";
            default:
                return null;
        }

    }

}
