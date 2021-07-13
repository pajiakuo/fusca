package com.earthshaker.fusca;

import com.alibaba.fastjson.JSON;
import com.earthshaker.fusca.remote.netty.bootstrap.*;
import com.earthshaker.fusca.remote.netty.common.MessagePack;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tk.mybatis.spring.annotation.MapperScan;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
@SpringBootApplication
@EnableTransactionManagement
//@MapperScan(basePackages = {"com.earthshaker.fusca.mapper"})
public class Application {
    public static void main(String[] args) {
        System.out.println("-----xxxxx------");
        System.out.println(System.getProperty("env", "65535"));
        SpringApplication.run(Application.class, args);
    }

    private void wsStart(){
        NettyRemotingWebSocketServer webSocketServer = new NettyRemotingWebSocketServer(new NettyServerConfig(),new ChannelEventOperatorListener(){

            @Override
            public void onChannelConnect(String remoteAddr, Channel channel) {
                log.info("onChannelConnect --- {}",remoteAddr);
            }

            @Override
            public void onChannelClose(String remoteAddr, Channel channel) {
                log.info("onChannelClose --- {}",remoteAddr);

            }

            @Override
            public void onChannelException(String remoteAddr, Channel channel) {
                log.info("onChannelException --- {}",remoteAddr);

            }

            @Override
            public void onChannelIdle(String remoteAddr, Channel channel) {
                log.info("onChannelIdle --- {}",remoteAddr);

            }
        });
        webSocketServer.registerProcessor(0, new AsyncNettyRequestProcessor() {
            @Override
            public MessagePack processRequest(ChannelHandlerContext ctx, MessagePack request) throws Exception {
                log.info("接收到ws-client 消息{}", JSON.toJSONString(request));
                request.setType(2);

                return request;
            }

            @Override
            public boolean rejectRequest() {
                return false;
            }
        }, Executors.newCachedThreadPool());
        webSocketServer.start();
    }
}
