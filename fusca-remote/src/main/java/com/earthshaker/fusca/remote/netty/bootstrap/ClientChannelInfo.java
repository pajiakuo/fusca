package com.earthshaker.fusca.remote.netty.bootstrap;

import io.netty.channel.Channel;

import java.io.Serializable;

/**
 * @Author: zhubo
 * @Description channel 无法序列化
 * @Date: 2021/7/7 4:27 下午
 */
public class ClientChannelInfo  {

    private final Channel channel;
    private final String clientId;
    private final int version;
    private volatile long lastUpdateTimestamp = System.currentTimeMillis();

    public ClientChannelInfo(Channel channel ,String clientId,int version){
        this.channel = channel;
        this.clientId = clientId ;
        this.version = version;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getClientId() {
        return clientId;
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

    public int getVersion() {
        return version;
    }
}
