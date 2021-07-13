package com.earthshaker.fusca.remote.netty.processor;

import com.earthshaker.fusca.remote.netty.bootstrap.NettyBootStrap;
import com.earthshaker.fusca.remote.netty.bootstrap.NettyRequestProcessor;
import com.earthshaker.fusca.remote.netty.common.MessagePack;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/7 3:38 下午
 */
public class SendMessageProcessor implements NettyRequestProcessor {

    protected final NettyBootStrap nettyBootStrap;

    public SendMessageProcessor(NettyBootStrap nettyBootStrap){
       this. nettyBootStrap = nettyBootStrap;
    }

    @Override
    public MessagePack processRequest(ChannelHandlerContext ctx, MessagePack request) throws Exception {
        //找到自己的
//        nettyBootStrap.getCustomerRelationMap();
//        nettyBootStrap.getOpRelationMap();
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
