package com.earthshaker.fusca.remote.netty.common.head;

import com.earthshaker.fusca.remote.exception.RemotingCommandException;
import com.earthshaker.fusca.remote.netty.common.PackHead;
import com.google.common.base.Strings;

import java.io.Serializable;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/7 7:34 下午
 */
public class ClientManagerRequestPackHead implements PackHead, Serializable {

    private String clientID;

    @Override
    public void checkFields() throws RemotingCommandException {
        if (Strings.isNullOrEmpty(clientID)){
            throw new RemotingCommandException("【clientID】参数异常");
        }
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }
}
