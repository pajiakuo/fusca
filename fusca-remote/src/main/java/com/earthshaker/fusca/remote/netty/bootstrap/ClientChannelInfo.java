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
    private final String sessionId;
    private final int version;
    private volatile long lastUpdateTimestamp = System.currentTimeMillis();
    private final String groupName;

    public ClientChannelInfo(Channel channel ,String clientId,int version,String sessionId,String groupName){
        this.channel = channel;
        this.clientId = clientId ;
        this.version = version;
        this.sessionId = sessionId;
        this.groupName = groupName;
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

    public String getSessionId() {
        return sessionId;
    }

    public String getGroupName() {
        return groupName;
    }
}
