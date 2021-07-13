package com.earthshaker.fusca.remote.constanst;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/8 7:48 下午
 */
public enum ResponseTypeEnum {

    DEFAULT(0),
    TEXT_WEB_SOCKET_FRAME(1);

    private Integer responseType;

    ResponseTypeEnum(Integer responseType){
        this.responseType = responseType;
    }

    public Integer getResponseType() {
        return responseType;
    }
}
