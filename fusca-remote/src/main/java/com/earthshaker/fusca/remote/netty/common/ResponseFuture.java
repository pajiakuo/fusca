package com.earthshaker.fusca.remote.netty.common;

import io.netty.channel.Channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/4 11:53 上午
 */
public class ResponseFuture {
    private final int opaque;
    private final Channel processChannel;
    private final long timeoutMillis;
    private final InvokeCallback invokeCallback;
    private final long beginTimestamp = System.currentTimeMillis();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private volatile MessagePack messagePack;
    private final SemaphoreReleaseOnlyOnce once;

    private volatile boolean sendRequestOK = true;
    private volatile Throwable cause;

    private final AtomicBoolean executeCallbackOnlyOnce = new AtomicBoolean(false);

    public ResponseFuture(Channel channel, int opaque, long timeoutMillis, InvokeCallback invokeCallback,
                          SemaphoreReleaseOnlyOnce once) {
        this.opaque = opaque;
        this.processChannel = channel;
        this.timeoutMillis = timeoutMillis;
        this.invokeCallback = invokeCallback;
        this.once = once;
    }
    public void executeInvokeCallback() {
        if (invokeCallback != null) {
            if (this.executeCallbackOnlyOnce.compareAndSet(false, true)) {
                invokeCallback.operationComplete(this);
            }
        }
    }

    public void release() {
        if (this.once != null) {
            this.once.release();
        }
    }
    public boolean isTimeout() {
        long diff = System.currentTimeMillis() - this.beginTimestamp;
        return diff > this.timeoutMillis;
    }


    public MessagePack waitResponse(final long timeoutMillis) throws InterruptedException {
        this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this.messagePack;
    }

    public void putResponse(final MessagePack messagePack) {
        this.messagePack = messagePack;
        this.countDownLatch.countDown();
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public boolean isSendRequestOK() {
        return sendRequestOK;
    }

    public void setSendRequestOK(boolean sendRequestOK) {
        this.sendRequestOK = sendRequestOK;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public InvokeCallback getInvokeCallback() {
        return invokeCallback;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public MessagePack getResponseCommand() {
        return messagePack;
    }

    public void setResponseCommand(MessagePack messagePack) {
        this.messagePack = messagePack;
    }

    public int getOpaque() {
        return opaque;
    }

    public Channel getProcessChannel() {
        return processChannel;
    }

    @Override
    public String toString() {
        return "ResponseFuture [responseCommand=" + messagePack
                + ", sendRequestOK=" + sendRequestOK
                + ", cause=" + cause
                + ", opaque=" + opaque
                + ", processChannel=" + processChannel
                + ", timeoutMillis=" + timeoutMillis
                + ", invokeCallback=" + invokeCallback
                + ", beginTimestamp=" + beginTimestamp
                + ", countDownLatch=" + countDownLatch + "]";
    }
}
