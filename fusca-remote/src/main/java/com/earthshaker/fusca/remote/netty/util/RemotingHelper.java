package com.earthshaker.fusca.remote.netty.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.earthshaker.fusca.remote.constanst.ResponseTypeEnum;
import com.earthshaker.fusca.remote.netty.common.MessagePack;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.net.SocketAddress;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/3 5:19 下午
 */
public class RemotingHelper {

    private static Log log = LogFactory.getCurrentLogFactory().getLog(RemotingHelper.class);

    public static String exceptionSimpleDesc(final Throwable e) {
        StringBuffer sb = new StringBuffer();
        if (e != null) {
            sb.append(e.toString());

            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                StackTraceElement elment = stackTrace[0];
                sb.append(", ");
                sb.append(elment.toString());
            }
        }

        return sb.toString();
    }

    public static String parseChannelRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }

        return "";
    }

    /**
     * websocket 返回
     * @param message
     * @return
     */
    public static ChannelFuture writeChannelResponse(ChannelHandlerContext ctx, MessagePack message ){
        if (ResponseTypeEnum.DEFAULT.getResponseType().equals(message.getResponseType())){
            return ctx.channel().writeAndFlush(message);
        }else if (ResponseTypeEnum.TEXT_WEB_SOCKET_FRAME.getResponseType().equals(message.getResponseType())){
            return ctx.channel().writeAndFlush( new TextWebSocketFrame(JSON.toJSONString(message)) );
        }else {
            log.warn("未知的返回值类型 {}",JSON.toJSONString(message));
            return ctx.channel().writeAndFlush(message);
        }

    }
    /**
     * websocket 返回
     * @param message
     * @return
     */
    public static ChannelFuture writeChannelResponse(Channel channel, MessagePack message ){
        if (ResponseTypeEnum.DEFAULT.getResponseType().equals(message.getResponseType())){
            return channel.writeAndFlush(message);
        }else if (ResponseTypeEnum.TEXT_WEB_SOCKET_FRAME.getResponseType().equals(message.getResponseType())){
            return channel.writeAndFlush( new TextWebSocketFrame(JSON.toJSONString(message)) );
        }else {
            log.warn("未知的返回值类型 {}",JSON.toJSONString(message));
            return channel.writeAndFlush(message);
        }

    }


}
