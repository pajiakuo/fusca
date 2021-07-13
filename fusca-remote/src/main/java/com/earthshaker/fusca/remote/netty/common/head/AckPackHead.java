package com.earthshaker.fusca.remote.netty.common.head;

import com.earthshaker.fusca.remote.exception.RemotingCommandException;
import com.earthshaker.fusca.remote.netty.common.PackHead;

import java.io.Serializable;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/7 4:19 下午
 */
public class AckPackHead  implements PackHead, Serializable {

    private Integer sourceOpaque;

    private String  remoteAddress;

    private Long responseTime;

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public Integer getSourceOpaque() {
        return sourceOpaque;
    }

    public void setSourceOpaque(Integer sourceOpaque) {
        this.sourceOpaque = sourceOpaque;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }

    @Override
    public void checkFields() throws RemotingCommandException {

    }
}
