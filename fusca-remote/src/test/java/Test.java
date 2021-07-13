import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.earthshaker.fusca.remote.SocketAddressUtil;
import com.earthshaker.fusca.remote.exception.RemotingConnectException;
import com.earthshaker.fusca.remote.exception.RemotingSendRequestException;
import com.earthshaker.fusca.remote.exception.RemotingTimeoutException;
import com.earthshaker.fusca.remote.netty.bootstrap.*;
import com.earthshaker.fusca.remote.netty.common.InvokeCallback;
import com.earthshaker.fusca.remote.netty.common.MessagePack;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.concurrent.Executors;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/4 5:49 下午
 */
public class Test {
    private static Log log = LogFactory.getCurrentLogFactory().getLog(Test.class);

    private static NettyRemotingServer remotingServer;
    private static NettyRemotingClient remotingClient;
    private static NettyRemotingWebSocketServer webSocketServer;

    @org.junit.Test
    public void testInvokeSync() throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
      final   MessagePack messagePack = new MessagePack();
        messagePack.setCode(0);
        messagePack.setAsync(false);
        messagePack.setBody("hello world");
        messagePack.setType(1);
        messagePack.setOpaque(123);
        MessagePack responseCmd =  remotingClient.invokeSync("localhost:8888", messagePack, 1000 * 120);
        Thread.sleep(2000);
        System.out.println(JSON.toJSONString(responseCmd));
        messagePack.setOpaque(1234);
        messagePack.setBody("hhhh");
        MessagePack responseCmd0 =  remotingClient.invokeSync("localhost:8888", messagePack, 1000 * 120);
        System.out.println(JSON.toJSONString(responseCmd0));
        Thread.sleep(60000);
        //remotingServer.invokeSync()
        System.out.println("---------------");
    }

    private static NettyRemotingServer creatServer(Integer port){
        NettyServerConfig config = new NettyServerConfig();
        if (port!=null){
            config.setListenPort(port);
        }
        NettyRemotingServer remotingServer = new NettyRemotingServer(config);
        remotingServer.registerProcessor(0, new AsyncNettyRequestProcessor() {
            @Override
            public MessagePack processRequest(ChannelHandlerContext ctx, MessagePack request) {
                log.info("接受到数据1{}", JSON.toJSONString(request));
                request.setRemark("Hi " + ctx.channel().remoteAddress());
                request.setBody(SocketAddressUtil.parseRemoteAddr(ctx.channel().remoteAddress()));
                request.setType(2);
                log.info("接受到数据2{}",JSON.toJSONString(request));
                return request;
            }

            @Override
            public boolean rejectRequest() {
                return false;
            }
        }, Executors.newCachedThreadPool());
        remotingServer.start();
        return remotingServer;
    }

    private static NettyRemotingClient creatClient(){
        NettyClientConfig nettyClientConfig = new NettyClientConfig();
        NettyRemotingClient client = new NettyRemotingClient(nettyClientConfig,new ChannelEventOperatorListener(){

            @Override
            public void onChannelConnect(String remoteAddr, Channel channel) {
                log.info("onChannelConnect----"+remoteAddr);
            }

            @Override
            public void onChannelClose(String remoteAddr, Channel channel) {
                log.info("onChannelClose----"+remoteAddr);

            }

            @Override
            public void onChannelException(String remoteAddr, Channel channel) {
                log.info("onChannelException----"+remoteAddr);

            }

            @Override
            public void onChannelIdle(String remoteAddr, Channel channel) {
                log.info("onChannelIdle----"+remoteAddr);
            }
        });
        client.start();
        return client;
    }
    @BeforeClass
    public static void setup() throws InterruptedException {
        log.info("----- setup");
        remotingServer = creatServer(null);
        remotingClient = creatClient();

    }

    @AfterClass
    public static void destroy() {
        log.info("----- destroy");
        remotingClient.shutdown();
        remotingServer.shutdown();
    }
}
