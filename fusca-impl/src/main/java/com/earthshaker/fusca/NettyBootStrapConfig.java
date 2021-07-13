package com.earthshaker.fusca;

import com.earthshaker.fusca.remote.netty.bootstrap.NettyBootStrap;
import com.earthshaker.fusca.remote.netty.bootstrap.NettyClientConfig;
import com.earthshaker.fusca.remote.netty.bootstrap.NettyServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/12 1:37 下午
 */
@Configuration
public class NettyBootStrapConfig {

    @Value("${fusca.remote.netty.address}")
    private String address;

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public NettyBootStrap nettyBootStrap() {
        NettyBootStrap nettyBootStrap = new NettyBootStrap(new NettyServerConfig(),new NettyClientConfig(),null,null,null);
        List<String> addressList = Arrays.asList(address.split(","));
        nettyBootStrap.registerClientConnectAddress(addressList);
        return nettyBootStrap;
    }
}
