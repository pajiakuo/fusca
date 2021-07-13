package com.earthshaker.fusca.remote.netty.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.earthshaker.fusca.remote.SocketAddressUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;


/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/3 10:13 上午
 */
public class NettyChannelUtil {

    private static Log log = LogFactory.getCurrentLogFactory().getLog(NettyChannelUtil.class);

    public static final String OS_NAME = System.getProperty("os.name");

    private static boolean isLinuxPlatform = false;

    private static boolean isWindowsPlatform = false;

    static {
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("linux")) {
            isLinuxPlatform = true;
        }

        if (OS_NAME != null && OS_NAME.toLowerCase().contains("windows")) {
            isWindowsPlatform = true;
        }
    }
    public static boolean isLinuxPlatform() {
        return isLinuxPlatform;
    }


    public static void closeChannel(Channel channel) {
        final String addrRemote = SocketAddressUtil.parseRemoteAddr(channel.remoteAddress()) ;
        channel.close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("closeChannel: close the connection to remote address[{}] result: {}", addrRemote,
                        future.isSuccess());
            }
        });
    }
}
