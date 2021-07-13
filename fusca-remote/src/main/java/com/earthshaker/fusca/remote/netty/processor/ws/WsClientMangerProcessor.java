package com.earthshaker.fusca.remote.netty.processor.ws;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.earthshaker.fusca.remote.RemotingSysResponseCode;
import com.earthshaker.fusca.remote.RequestCode;
import com.earthshaker.fusca.remote.netty.bootstrap.ClientChannelInfo;
import com.earthshaker.fusca.remote.netty.bootstrap.NettyBootStrap;
import com.earthshaker.fusca.remote.netty.bootstrap.NettyRequestProcessor;
import com.earthshaker.fusca.remote.netty.common.MessagePack;
import com.earthshaker.fusca.remote.netty.common.head.ClientAckPackHead;
import com.earthshaker.fusca.remote.netty.common.head.WebSocketHeadPackHead;
import com.earthshaker.fusca.remote.netty.processor.ClientManagerProcessor;
import com.earthshaker.fusca.remote.netty.util.RemotingHelper;
import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/10 11:02 下午
 */
public class WsClientMangerProcessor implements NettyRequestProcessor {
    protected final NettyBootStrap nettyBootStrap;

    private static final Long TIME_OUT = 1000*20L;

    public WsClientMangerProcessor(NettyBootStrap nettyBootStrap){
        this.nettyBootStrap = nettyBootStrap;
    }
    private static Log log = LogFactory.getCurrentLogFactory().getLog(ClientManagerProcessor.class);

    public static final String HEART_BEAT = "heartbeat";
    @Override
    public MessagePack processRequest(ChannelHandlerContext ctx, MessagePack request) throws Exception {
        if (RequestCode.HEART_BEAT_WS==request.getCode()){
            WebSocketHeadPackHead head =
                JSONObject.parseObject(JSON.toJSONString(request.getHead()),WebSocketHeadPackHead.class);
            ClientChannelInfo clientChannelInfo = new ClientChannelInfo(ctx.channel(),head.getClientId(),request.getVersion());
            ClientAckPackHead ackPackHead = new ClientAckPackHead();
            ackPackHead.setRemoteAddress(RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
            ackPackHead.setSourceOpaque(request.getOpaque());
            ackPackHead.setResponseTime(System.currentTimeMillis());
            ackPackHead.setRequestCode(request.getCode());
            MessagePack messagePack0 = MessagePack.createResponseCommand(RemotingSysResponseCode.SUCCESS,ackPackHead,null);
            messagePack0.setBody(HEART_BEAT);
            messagePack0.setBeginTimestamp(request.getBeginTimestamp());
            registerClientChannel(clientChannelInfo,head.getSessionId(),nettyBootStrap.groupName(head.getSource()));

            return messagePack0;
        }else if (RequestCode.UNREGISTER_CLIENT_WS==request.getCode()){
            WebSocketHeadPackHead head =
                    JSONObject.parseObject(JSON.toJSONString(request.getHead()),WebSocketHeadPackHead.class);
                ClientChannelInfo clientChannelInfo = new ClientChannelInfo(
                        ctx.channel(),
                        head.getClientId(),
                        request.getVersion());
                //服务下线该客户端
                unRegisterClientChannel(clientChannelInfo,head.getSessionId(),nettyBootStrap.groupName(head.getSource()));
                ClientAckPackHead ackPackHead = new ClientAckPackHead();
                ackPackHead.setRemoteAddress(RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
                ackPackHead.setSourceOpaque(request.getOpaque());
                ackPackHead.setResponseTime(System.currentTimeMillis());
                ackPackHead.setClientID(head.getClientId());
                ackPackHead.setRequestCode(request.getCode());
                MessagePack messagePack0 = MessagePack.createResponseCommand(RemotingSysResponseCode.SUCCESS,ackPackHead,null);
                messagePack0.setBeginTimestamp(request.getBeginTimestamp());
                return messagePack0;
        }else {
            //throw new RemotingCommandException("未知请求处理流程");
        }
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }

    public synchronized void registerClientChannel(ClientChannelInfo clientChannelInfo,String sessionId,String groupName){

        if (groupName==null){
            //
        }else {
            ConcurrentHashMap<String, List<ClientChannelInfo>> groupMap =  nettyBootStrap.getGroupClientChannelMap().get(groupName);
            if (groupMap==null){
                groupMap = new ConcurrentHashMap<>();
                List<ClientChannelInfo> clientChannels = Lists.newArrayList();
                clientChannels.add(clientChannelInfo);
                groupMap.put(sessionId,clientChannels);
                nettyBootStrap.getGroupClientChannelMap().put(groupName,groupMap);
            }else {
                List<ClientChannelInfo> clientChannels = groupMap.get(sessionId);
                if (CollectionUtil.isEmpty(clientChannels)){
                    clientChannels = Lists.newArrayList();
                    clientChannels.add(clientChannelInfo);
                    nettyBootStrap.getGroupClientChannelMap().get(groupName).put(sessionId,clientChannels);
                }else {
                    Iterator<ClientChannelInfo> iterator =  clientChannels.iterator();
                    boolean flag = true;
                    while (iterator.hasNext()){
                        ClientChannelInfo itNext = iterator.next();
                        if (itNext.getClientId().equals(clientChannelInfo.getClientId())){
                            flag = false;
                            break;
                        }
                    }
                    if (flag){
                        clientChannels.add(clientChannelInfo);
                    }
                }
            }

        }

        ConcurrentHashMap<Channel, ClientChannelInfo> insideClientChannelInfoMap = nettyBootStrap.managerClientChannelManager(nettyBootStrap.WS_OUTSIDE_GROUP);
        if (insideClientChannelInfoMap==null){
            insideClientChannelInfoMap = new ConcurrentHashMap<>();
            nettyBootStrap.clientChannelManager.put(nettyBootStrap.WS_OUTSIDE_GROUP,insideClientChannelInfoMap);
        }
        ClientChannelInfo clientChannelInfo0 = insideClientChannelInfoMap.get(clientChannelInfo.getClientId());
        if (clientChannelInfo0==null){
            insideClientChannelInfoMap.put(clientChannelInfo.getChannel(),clientChannelInfo);
        }else {
            clientChannelInfo0.setLastUpdateTimestamp(System.currentTimeMillis());
        }
    }

    public synchronized void unRegisterClientChannel(ClientChannelInfo clientChannelInfo,String sessionId,String groupName){

        if (groupName==null){
            //
        }else {
            ConcurrentHashMap<String, List<ClientChannelInfo>> groupMap =  nettyBootStrap.getGroupClientChannelMap().get(groupName);
            if (CollectionUtil.isNotEmpty(groupMap)){
                List<ClientChannelInfo> clientChannels = groupMap.get(sessionId);
                if (CollectionUtil.isNotEmpty(clientChannels)){
                    Iterator<ClientChannelInfo> iterator =  clientChannels.iterator();
                    while (iterator.hasNext()){
                        ClientChannelInfo itNext = iterator.next();
                        if (itNext.getClientId().equals(clientChannelInfo.getClientId())){
                            iterator.remove();
                            break;
                        }
                    }
                }
            }
        }
        ConcurrentHashMap<Channel, ClientChannelInfo> insideClientChannelInfoMap = nettyBootStrap.managerClientChannelManager(nettyBootStrap.WS_OUTSIDE_GROUP);
        if (insideClientChannelInfoMap!=null&&!insideClientChannelInfoMap.isEmpty()){
            insideClientChannelInfoMap.remove(clientChannelInfo.getChannel());
            log.warn("解绑注册 Server channel {}",clientChannelInfo.getClientId());
        }

    }






}
