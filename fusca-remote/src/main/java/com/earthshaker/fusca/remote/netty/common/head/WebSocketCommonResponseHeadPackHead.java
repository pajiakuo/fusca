package com.earthshaker.fusca.remote.netty.common.head;

import com.earthshaker.fusca.remote.exception.RemotingCommandException;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/6 4:42 下午
 */
public class WebSocketCommonResponseHeadPackHead extends WebSocketHeadPackHead {

    private Long responseTimestamp;

    @Override
    public void checkFields() throws RemotingCommandException {
        checkWebSocketFields();
    }

    public Long getResponseTimestamp() {
        return responseTimestamp;
    }

    public void setResponseTimestamp(Long responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }
}
