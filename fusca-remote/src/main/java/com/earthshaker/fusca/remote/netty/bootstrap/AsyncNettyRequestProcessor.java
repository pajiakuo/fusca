package com.earthshaker.fusca.remote.netty.bootstrap;

import com.earthshaker.fusca.remote.netty.common.MessagePack;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/4 8:06 下午
 */
public abstract class AsyncNettyRequestProcessor implements NettyRequestProcessor {

    public void asyncProcessRequest(ChannelHandlerContext ctx, MessagePack request, RemotingResponseCallback responseCallback) throws Exception {
        MessagePack response = processRequest(ctx, request);
        responseCallback.callback(response);
    }
}
