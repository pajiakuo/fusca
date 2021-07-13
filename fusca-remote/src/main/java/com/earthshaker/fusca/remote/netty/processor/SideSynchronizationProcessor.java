package com.earthshaker.fusca.remote.netty.processor;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.earthshaker.fusca.remote.RemotingSysResponseCode;
import com.earthshaker.fusca.remote.netty.bootstrap.AsyncNettyRequestProcessor;
import com.earthshaker.fusca.remote.netty.bootstrap.ClientChannelInfo;
import com.earthshaker.fusca.remote.netty.bootstrap.NettyBootStrap;
import com.earthshaker.fusca.remote.netty.common.MessagePack;
import com.earthshaker.fusca.remote.netty.common.head.SideSynchronizationHead;
import com.earthshaker.fusca.remote.netty.util.RemotingHelper;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/12 6:06 下午
 */
public class SideSynchronizationProcessor extends AsyncNettyRequestProcessor {

    private static Log log = LogFactory.getCurrentLogFactory().getLog(SideSynchronizationProcessor.class);

    protected final NettyBootStrap nettyBootStrap;

    public SideSynchronizationProcessor(NettyBootStrap nettyBootStrap){
        this. nettyBootStrap = nettyBootStrap;
    }
    @Override
    public MessagePack processRequest(ChannelHandlerContext ctx, MessagePack request) throws Exception {
        log.warn("接收到多端同步消息{}",request.toString());
        SideSynchronizationHead sideSynchronizationHead = JSONObject.parseObject(JSON.toJSONString(request.getHead()),SideSynchronizationHead.class);
        //多端同步
        List<ClientChannelInfo> clientChannels =  nettyBootStrap.getGroupClientChannelMap().get(nettyBootStrap.groupName(sideSynchronizationHead.getSource())).get(sideSynchronizationHead.getSessionId());
        if (CollectionUtil.isNotEmpty(clientChannels)){
            if (CollectionUtil.isNotEmpty(sideSynchronizationHead.getExcludeClientIds())){
                for (ClientChannelInfo clientChannelInfo: clientChannels){
                    if (!sideSynchronizationHead.getExcludeClientIds().contains(clientChannelInfo.getClientId())){
                        MessagePack messagePack = MessagePack.createResponseCommand(RemotingSysResponseCode.SUCCESS,sideSynchronizationHead,"", request.getResponseType());
                        messagePack.setBeginTimestamp(request.getBeginTimestamp());
                        messagePack.setBody(request.getBody());
                        messagePack.setVersion(request.getVersion());
                        messagePack.setExtFields(request.getExtFields());
                        messagePack.setMessageId(request.getMessageId());
                        RemotingHelper.writeChannelResponse(clientChannelInfo.getChannel(),messagePack);
                    }
                }
            }
        }else {
            //没有需要同步的客户端
            log.warn("没有需要端同步的客户端{}",sideSynchronizationHead.getSessionId());
        }
        MessagePack messagePack = MessagePack.createResponseCommand(RemotingSysResponseCode.SUCCESS,sideSynchronizationHead,"");
        return messagePack;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
