package com.earthshaker.fusca.remote.netty.common.head;

import lombok.Data;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/7 7:31 下午
 */
@Data
public class ClientAckPackHead extends AckPackHead {
    private String clientID;
    private int requestCode;
}
