package com.earthshaker.fusca.remote.netty.common;

import com.earthshaker.fusca.remote.exception.RemotingCommandException;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/3 1:21 下午
 */
public interface PackHead {

    void checkFields() throws RemotingCommandException;

}
