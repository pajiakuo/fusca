package com.earthshaker.fusca.remote.netty.common;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/4 11:56 上午
 */
public interface InvokeCallback {
    void operationComplete(final ResponseFuture responseFuture);
}
