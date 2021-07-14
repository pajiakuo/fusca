package com.earthshaker.fusca.remote.netty.processor;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.earthshaker.fusca.remote.RemotingSysResponseCode;
import com.earthshaker.fusca.remote.RequestCode;
import com.earthshaker.fusca.remote.exception.RemotingCommandException;
import com.earthshaker.fusca.remote.netty.bootstrap.ClientChannelInfo;
import com.earthshaker.fusca.remote.netty.bootstrap.NettyBootStrap;
import com.earthshaker.fusca.remote.netty.bootstrap.NettyRequestProcessor;
import com.earthshaker.fusca.remote.netty.common.MessagePack;
import com.earthshaker.fusca.remote.netty.common.head.ClientAckPackHead;
import com.earthshaker.fusca.remote.netty.common.head.ClientManagerRequestPackHead;
import com.earthshaker.fusca.remote.netty.util.RemotingHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/4 5:35 下午
 */
public class    ClientManagerProcessor implements NettyRequestProcessor {
    protected final NettyBootStrap nettyBootStrap;

    private static final Long TIME_OUT = 1000*20L;

    public ClientManagerProcessor(NettyBootStrap nettyBootStrap){
        this.nettyBootStrap = nettyBootStrap;
    }
    private static Log log = LogFactory.getCurrentLogFactory().getLog(ClientManagerProcessor.class);

    public static final String HEART_BEAT = "heartbeat";
    @Override
    public MessagePack processRequest(ChannelHandlerContext ctx, MessagePack request) throws Exception {
        if (RequestCode.HEART_BEAT==request.getCode()){
            if (request.getHead() instanceof ClientManagerRequestPackHead){
                ClientManagerRequestPackHead head = (ClientManagerRequestPackHead) request.getHead();
                ClientChannelInfo clientChannelInfo = new ClientChannelInfo(ctx.channel(),head.getClientID(),request.getVersion(),head.getClientID(),"");
                ClientAckPackHead ackPackHead = new ClientAckPackHead();
                ackPackHead.setRemoteAddress(RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
                ackPackHead.setSourceOpaque(request.getOpaque());
                ackPackHead.setResponseTime(System.currentTimeMillis());
                ackPackHead.setRequestCode(request.getCode());
                MessagePack messagePack0 = MessagePack.createResponseCommand(RemotingSysResponseCode.SUCCESS,ackPackHead,null);
                messagePack0.setBody(HEART_BEAT);
                messagePack0.setBeginTimestamp(request.getBeginTimestamp());
                registerClientChannel(clientChannelInfo);
                return messagePack0;
            }else {
                throw new RemotingCommandException("心跳请求头异常");
            }
        }else if (RequestCode.UNREGISTER_CLIENT==request.getCode()){
            if (request.getHead() instanceof ClientManagerRequestPackHead){
                ClientManagerRequestPackHead head = (ClientManagerRequestPackHead) request.getHead();
                ClientChannelInfo clientChannelInfo = new ClientChannelInfo(
                        ctx.channel(),
                        head.getClientID(),
                        request.getVersion(),head.getClientID(),"");
                //服务下线该客户端
                unRegisterClientChannel(clientChannelInfo);
                ClientAckPackHead ackPackHead = new ClientAckPackHead();
                ackPackHead.setRemoteAddress(RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
                ackPackHead.setSourceOpaque(request.getOpaque());
                ackPackHead.setResponseTime(System.currentTimeMillis());
                ackPackHead.setClientID(head.getClientID());
                ackPackHead.setRequestCode(request.getCode());
                MessagePack messagePack0 = MessagePack.createResponseCommand(RemotingSysResponseCode.SUCCESS,ackPackHead,null);
                messagePack0.setBeginTimestamp(request.getBeginTimestamp());
                return messagePack0;
            }else {
                throw new RemotingCommandException("心跳请求头异常");
            }
        }else {
            //throw new RemotingCommandException("未知请求处理流程");
        }
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }

    public synchronized void registerClientChannel(ClientChannelInfo clientChannelInfo){
        ConcurrentHashMap<Channel, ClientChannelInfo> insideClientChannelInfoMap = nettyBootStrap.managerClientChannelManager(nettyBootStrap.INSIDE_GROUP);
        if (insideClientChannelInfoMap==null){
            insideClientChannelInfoMap = new ConcurrentHashMap<>();
            nettyBootStrap.clientChannelManager.put(nettyBootStrap.INSIDE_GROUP,insideClientChannelInfoMap);
        }
        ClientChannelInfo clientChannelInfo0 = insideClientChannelInfoMap.get(clientChannelInfo.getClientId());
        if (clientChannelInfo0==null){
            insideClientChannelInfoMap.put(clientChannelInfo.getChannel(),clientChannelInfo);
        }else {
            clientChannelInfo0.setLastUpdateTimestamp(System.currentTimeMillis());
        }
    }

    public synchronized void unRegisterClientChannel(ClientChannelInfo clientChannelInfo){
        ConcurrentHashMap<Channel, ClientChannelInfo> insideClientChannelInfoMap = nettyBootStrap.managerClientChannelManager(nettyBootStrap.INSIDE_GROUP);
        if (insideClientChannelInfoMap!=null&&!insideClientChannelInfoMap.isEmpty()){
            insideClientChannelInfoMap.remove(clientChannelInfo.getChannel());
            log.warn("解绑注册 Server channel {}",clientChannelInfo.getClientId());
        }
    }
}
