package com.earthshaker.fusca.remote.netty.common;

import com.alibaba.fastjson.JSONObject;
import com.earthshaker.fusca.remote.constanst.ResponseTypeEnum;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/3 1:19 下午
 *  序列化方式 暂时用java 自带的
 */
public class MessagePack<T> implements Serializable {

    /**
     * 请求代码/响应代码
     */
    private int code;

    /**
     * 请求头
     */
    private T head;

    /**
     * 1 请求 2 响应
     */
    private Integer type;

    /**
     * 是否异步 true
     */
    private Boolean async;

    private int opaque = requestId.getAndIncrement();

    private static AtomicInteger requestId = new AtomicInteger(0);

    private int version = 0;

    private String remark;

    private static volatile int configVersion = -1;

    private String body;

    private Long beginTimestamp;

    private String messageId;


    /**
     * 响应类型  MessagePack /TextWebSocketFrame
     */
    private Integer responseType = 0;


    /**
     * 拓展自定义参数
     */
    private HashMap<String, String> extFields;



    public MessagePack(){

    }
    public static <E>MessagePack createAsyncRequestCommand(int code, E head) {
        MessagePack cmd = new MessagePack();
        cmd.setCode(code);
        cmd.head = head;
        cmd.setType(1);
        cmd.setBeginTimestamp(System.currentTimeMillis());
        cmd.setAsync(true);
        return cmd;
    }
    public static <E>MessagePack createSyncRequestCommand(int code, E head) {
        MessagePack cmd = new MessagePack();
        cmd.setCode(code);
        cmd.head = head;
        cmd.setType(1);
        cmd.setBeginTimestamp(System.currentTimeMillis());
        cmd.setAsync(false);
        return cmd;
    }

    public static <E>MessagePack createResponseCommand(int code, E head,String remark,Integer responseType) {
        MessagePack cmd = new MessagePack();
        cmd.setResponseType(responseType==null?ResponseTypeEnum.DEFAULT.getResponseType():responseType);
        cmd.setCode(code);
        cmd.head = head;
        cmd.setRemark(remark);
        return cmd;
    }
    public static <E>MessagePack createResponseCommand(int code, E head,String remark) {
        MessagePack cmd = new MessagePack();
        cmd.setResponseType(ResponseTypeEnum.DEFAULT.getResponseType());
        cmd.setCode(code);
        cmd.type = 2;
        cmd.head = head;
        cmd.setRemark(remark);
        return cmd;
    }
    /**
     * 解析ws-client数据
     * @param content
     * @return
     */
    public static MessagePack parseRequestCommand(String content) {
        MessagePack cmd = new MessagePack();
        cmd.setType(1);
        MessagePack message = JSONObject.parseObject(content,MessagePack.class);
        message.setAsync(message.getAsync()==null?false:message.getAsync());
        message.setOpaque(cmd.getOpaque());
        cmd =null;
        return message;
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getOpaque() {
        return opaque;
    }

    public void setOpaque(int opaque) {
        this.opaque = opaque;
    }

    public HashMap<String, String> getExtFields() {
        return extFields;
    }

    public void setExtFields(HashMap<String, String> extFields) {
        this.extFields = extFields;
    }

    public void addExtField(String key, String value) {
        if (null == extFields) {
            extFields = new HashMap<String, String>();
        }
        extFields.put(key, value);
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public T getHead() {
        return head;
    }

    public void setHead(T head) {
        this.head = head;
    }

    public Long getBeginTimestamp() {
        return beginTimestamp;
    }

    public void setBeginTimestamp(Long beginTimestamp) {
        this.beginTimestamp = beginTimestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Integer getResponseType() {
        return responseType;
    }

    public void setResponseType(Integer responseType) {
        this.responseType = responseType;
    }


    public String toString() {
        return "MessagePack [messageId=" + messageId + ", opaque=" + opaque
                +  ", beginTimestamp=" + beginTimestamp + ", async=" + async + ", extFields=" + extFields +
                ", type=" + type + ", responseType=" + responseType + ", code="
                + code + ", body=" + body + ", version=" + version
                + ", remark=" + remark + ", head="+ JSONObject.toJSONString(head)+"]";
    }
}
