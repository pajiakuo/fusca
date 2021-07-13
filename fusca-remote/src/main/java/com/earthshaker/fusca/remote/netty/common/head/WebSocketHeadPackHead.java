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
     * 消息开始时间 前端处理
     */
    private Long beginTimestamp;

    /**
     * 消息唯一标示
     */
    private String messageId;

    /**
     * 密文
     */
    private String cna;

    /**
     * 发送给谁
     */
    private String to;

    public Long getBeginTimestamp() {
        return beginTimestamp;
    }

    public void setBeginTimestamp(Long beginTimestamp) {
        this.beginTimestamp = beginTimestamp;
    }

    public String getCna() {
        return cna;
    }

    public void setCna(String cna) {
        this.cna = cna;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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
        if (beginTimestamp==null){
            throw new  RemotingCommandException("非法参数【beginTimestamp】");
        }
    }


    @Override
    public void checkFields() throws RemotingCommandException {

    }
}
