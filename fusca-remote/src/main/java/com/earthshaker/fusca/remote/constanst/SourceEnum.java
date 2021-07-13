package com.earthshaker.fusca.remote.constanst;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/12 2:45 下午
 * 来源 1-门户 2-Op 3-游客"
 */
public enum  SourceEnum {

    TOURIST(3),

    OP_USER(2),

    CUSTOMER(1) ;

    private Integer source;

    SourceEnum(Integer source){
        this.source = source;
    }

    public Integer getSource() {
        return source;
    }
}
