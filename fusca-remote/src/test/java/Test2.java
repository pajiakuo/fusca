import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson.JSON;
import com.earthshaker.fusca.remote.RequestCode;
import com.earthshaker.fusca.remote.constanst.ResponseTypeEnum;
import com.earthshaker.fusca.remote.netty.common.MessagePack;
import com.earthshaker.fusca.remote.netty.common.head.ClientManagerRequestPackHead;
import com.earthshaker.fusca.remote.netty.common.head.WebSocketHeadPackHead;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/7/8 10:32 上午
 */
public class Test2 {
    public static void main(String[] args) {
//        ClientManagerRequestPackHead heartBeatHead = new ClientManagerRequestPackHead();
//        heartBeatHead.setClientID("123333333");
//        MessagePack messagePack = MessagePack.createAsyncRequestCommand(0,heartBeatHead);
//        messagePack.setBeginTimestamp(System.currentTimeMillis());
//        messagePack.setBody("hhhhhhh");
//
//        System.out.println(JSON.toJSONString(messagePack));
//
//
//        WebSocketHeadPackHead webSocketHeadPackHead = new WebSocketHeadPackHead();
//        webSocketHeadPackHead.setBeginTimestamp(System.currentTimeMillis());
//        webSocketHeadPackHead.setMessageId(UUID.fastUUID().toString());
//        webSocketHeadPackHead.setSource(1);
//        webSocketHeadPackHead.setSessionId(UUID.fastUUID().toString());
//        MessagePack messagePack0 = MessagePack.createAsyncRequestCommand(RequestCode.SEND_MESSAGE_WS,webSocketHeadPackHead);
//        messagePack0.setResponseType(ResponseTypeEnum.TEXT_WEB_SOCKET_FRAME.getResponseType());
//        System.out.println(JSON.toJSONString(messagePack0));

        String sessionId = "56d76890-62e4-41af-857c-a310459962ce";
        WebSocketHeadPackHead webSocketHeadPackHead0 = new WebSocketHeadPackHead();
        webSocketHeadPackHead0.setSessionId(sessionId);
        webSocketHeadPackHead0.setSource(3);

        webSocketHeadPackHead0.setClientId(UUID.fastUUID().toString());
        MessagePack messagePack0 = MessagePack.createAsyncRequestCommand(RequestCode.HEART_BEAT_WS,webSocketHeadPackHead0);
        messagePack0.setResponseType(1);
        messagePack0.setMessageId(UUID.fastUUID().toString());
        messagePack0.setBeginTimestamp(System.currentTimeMillis());
        messagePack0.setBody("heartbeat");
        System.out.println(JSON.toJSONString(messagePack0));
        MessagePack messagePack1 = MessagePack.createAsyncRequestCommand(RequestCode.SEND_MESSAGE_WS,webSocketHeadPackHead0);
        messagePack1.setResponseType(1);
        messagePack1.setBody("游客消息多端同步");
        messagePack1.setMessageId(UUID.fastUUID().toString());
        messagePack1.setBeginTimestamp(System.currentTimeMillis());
        System.out.println(JSON.toJSONString(messagePack1));


    }
}
