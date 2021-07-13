package com.earthshaker.fusca.remote.netty.common.head;

import com.earthshaker.fusca.remote.exception.RemotingCommandException;
import com.earthshaker.fusca.remote.netty.common.PackHead;
import com.google.common.base.Strings;

import java.io.Serializable;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/6 3:53 下午
 */
public class WebSocketHeadPackHead implements PackHead, Serializable {

    private String clientId;

    /**
     * 来源 1-门户 2-Op 3-游客"
     */
    private Integer source;

    /**
     * 与source 组合使用 为 3时 是游客设备id
     */
    private String sessionId;

    /**
     * 密文
     */
    private String cna;

    /**
     * 发送给谁
     */
    private String to;

    public String getCna() {
        return cna;
    }

    public void setCna(String cna) {
        this.cna = cna;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    void checkWebSocketFields() throws RemotingCommandException{
        if (source==null){
            throw new  RemotingCommandException("非法参数【source】");
        }
        if (Strings.isNullOrEmpty(sessionId)){
            throw new  RemotingCommandException("非法参数【sessionId】");
        }
    }


    @Override
    public void checkFields() throws RemotingCommandException {

    }
}
