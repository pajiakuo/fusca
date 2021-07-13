package com.earthshaker.fusca.remote.netty.processor.ws;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.earthshaker.fusca.remote.RemotingSysResponseCode;
import com.earthshaker.fusca.remote.RequestCode;
import com.earthshaker.fusca.remote.constanst.ResponseTypeEnum;
import com.earthshaker.fusca.remote.constanst.SourceEnum;
import com.earthshaker.fusca.remote.exception.RemotingConnectException;
import com.earthshaker.fusca.remote.exception.RemotingSendRequestException;
import com.earthshaker.fusca.remote.exception.RemotingTimeoutException;
import com.earthshaker.fusca.remote.exception.RemotingTooMuchRequestException;
import com.earthshaker.fusca.remote.netty.bootstrap.ClientChannelInfo;
import com.earthshaker.fusca.remote.netty.bootstrap.NettyBootStrap;
import com.earthshaker.fusca.remote.netty.bootstrap.NettyRequestProcessor;
import com.earthshaker.fusca.remote.netty.common.InvokeCallback;
import com.earthshaker.fusca.remote.netty.common.MessagePack;
import com.earthshaker.fusca.remote.netty.common.ResponseFuture;
import com.earthshaker.fusca.remote.netty.common.head.SideSynchronizationHead;
import com.earthshaker.fusca.remote.netty.common.head.WebSocketCommonResponseHeadPackHead;
import com.earthshaker.fusca.remote.netty.common.head.WebSocketHeadPackHead;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Map;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/7 3:38 下午
 */
public class WsSendMessageProcessor implements NettyRequestProcessor {

    private static Log log = LogFactory.getCurrentLogFactory().getLog(WsSendMessageProcessor.class);

    protected final NettyBootStrap nettyBootStrap;

    public WsSendMessageProcessor(NettyBootStrap nettyBootStrap){
       this. nettyBootStrap = nettyBootStrap;
    }

    @Override
    public MessagePack processRequest(ChannelHandlerContext ctx, MessagePack request) throws Exception {
        //找到自己的
        log.info("接收到ws-client 消息{}", JSON.toJSONString(request));
        Map<String, String> customerRelationMap =  nettyBootStrap.getCustomerRelationMap();
        Map<String, String> opRelationMap = nettyBootStrap.getOpRelationMap();
        SideSynchronizationHead webSocketHeadPackHead = JSONObject.parseObject(JSON.toJSONString(request.getHead()),SideSynchronizationHead.class);
        webSocketHeadPackHead.setClientId(nettyBootStrap.getClientId());
        //多端同步
        List<ClientChannelInfo> clientChannels =  nettyBootStrap.getGroupClientChannelMap().get(nettyBootStrap.groupName(webSocketHeadPackHead.getSource())).get(webSocketHeadPackHead.getSessionId());
        if (SourceEnum.CUSTOMER.getSource().equals(webSocketHeadPackHead.getSource())
        ||SourceEnum.TOURIST.getSource().equals(webSocketHeadPackHead.getSource())){
            //发送自己同步消息
            //发送给销售
            //保存消息到db
            syncSendClientMessageToAllServer(webSocketHeadPackHead,request);
        }else if(SourceEnum.OP_USER.getSource().equals(webSocketHeadPackHead.getSource())){
            //发送给集群中的自己 （多端同步）
            //发送给消息结收者 根据消息中发送给谁

        }else{
            //todo 未知的类型
            return null;
        }

        WebSocketCommonResponseHeadPackHead packHead = new WebSocketCommonResponseHeadPackHead();
        packHead.setResponseTimestamp(System.currentTimeMillis());
        packHead.setSource(webSocketHeadPackHead.getSource());
        packHead.setSessionId(webSocketHeadPackHead.getSessionId());
        MessagePack messagePack0 = MessagePack.createResponseCommand(RemotingSysResponseCode.SUCCESS,packHead,"", ResponseTypeEnum.TEXT_WEB_SOCKET_FRAME.getResponseType());
        messagePack0.setBeginTimestamp(request.getBeginTimestamp());
        messagePack0.setMessageId(request.getMessageId());
        return messagePack0;
    }


//    public void asyncSendClientMessageToAllServer(SideSynchronizationHead webSocketHeadPackHead ,MessagePack request){
//        if (CollectionUtil.isNotEmpty(nettyBootStrap.getSuccessAddrList().get())){
//            for (String address:nettyBootStrap.getSuccessAddrList().get()){
//                try {
//                    MessagePack messagePack = MessagePack.createAsyncRequestCommand(RequestCode.SIDE_SYNC_CLIENT,webSocketHeadPackHead);
//                    List<String> excludeClientIds = Lists.newArrayList();
//                    excludeClientIds.add(webSocketHeadPackHead.getClientId());
//                    webSocketHeadPackHead.setExcludeClientIds(excludeClientIds);
//                    messagePack.setBeginTimestamp(request.getBeginTimestamp());
//                    messagePack.setBody(request.getBody());
//                    messagePack.setVersion(request.getVersion());
//                    messagePack.setExtFields(request.getExtFields());
//                    messagePack.setResponseType(request.getResponseType());
//                    messagePack.setMessageId(request.getMessageId());
//                    nettyBootStrap.getNettyRemotingClient().invokeAsync(address,messagePack,3000,new InvokeCallback(){
//                        @Override
//                        public void operationComplete(ResponseFuture responseFuture) {
//                            if (!responseFuture.isSendRequestOK()){
//                                log.warn("async send ClientMessage to "+address+" fail"+messagePack.toString());
//                            }else {
//
//                            }
//                        }
//                    });
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } catch (RemotingConnectException e) {
//                    e.printStackTrace();
//                } catch (RemotingTooMuchRequestException e) {
//                    e.printStackTrace();
//                } catch (RemotingTimeoutException e) {
//                    e.printStackTrace();
//                } catch (RemotingSendRequestException e) {
//                    e.printStackTrace();
//                }
//            }
//        }else {
//            log.warn("no server can send async message");
//        }
//
//
//    }

    public void syncSendClientMessageToAllServer(SideSynchronizationHead webSocketHeadPackHead ,MessagePack request){
        if (CollectionUtil.isNotEmpty(nettyBootStrap.getSuccessAddrList().get())){
            for (String address:nettyBootStrap.getSuccessAddrList().get()){
                try {
                    MessagePack messagePack = MessagePack.createAsyncRequestCommand(RequestCode.SIDE_SYNC_CLIENT,webSocketHeadPackHead);
                    List<String> excludeClientIds = Lists.newArrayList();
                    excludeClientIds.add(webSocketHeadPackHead.getClientId());
                    webSocketHeadPackHead.setExcludeClientIds(excludeClientIds);
                    messagePack.setBeginTimestamp(request.getBeginTimestamp());
                    messagePack.setBody(request.getBody());
                    messagePack.setVersion(request.getVersion());
                    messagePack.setExtFields(request.getExtFields());
                    messagePack.setResponseType(ResponseTypeEnum.DEFAULT.getResponseType());
                    messagePack.setMessageId(request.getMessageId());
                    nettyBootStrap.getNettyRemotingClient().invokeSync(address,messagePack,3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (RemotingConnectException e) {
                    e.printStackTrace();
                } catch (RemotingTimeoutException e) {
                    e.printStackTrace();
                } catch (RemotingSendRequestException e) {
                    e.printStackTrace();
                }
            }
        }else {
            log.warn("no server can send async message");
        }


    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
