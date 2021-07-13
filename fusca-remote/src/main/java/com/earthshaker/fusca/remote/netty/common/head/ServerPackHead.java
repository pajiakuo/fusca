package com.earthshaker.fusca.remote.netty.common.head;

import com.earthshaker.fusca.remote.exception.RemotingCommandException;
import com.earthshaker.fusca.remote.netty.common.PackHead;

import java.io.Serializable;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/4 12:48 下午
 */
class ServerAbstractPackHead implements PackHead, Serializable {

    protected String serverAddr;


    protected  ServerAbstractPackHead(String serverAddr){
        this.serverAddr = serverAddr;
    }
    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }
}
