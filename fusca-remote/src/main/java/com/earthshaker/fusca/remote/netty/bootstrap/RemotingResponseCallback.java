package com.earthshaker.fusca.remote.netty.bootstrap;

import com.earthshaker.fusca.remote.netty.common.MessagePack;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/4 8:08 下午
 */
public interface RemotingResponseCallback {
    void callback(MessagePack response);
}
