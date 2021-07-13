

package com.earthshaker.fusca.remote;

public class RequestCode {


    public static final int SEND_MESSAGE = 10;

    /**
     * ClientManagerRequestPackHead
     *
     */
    public static final int HEART_BEAT = 11;


    public static final int UNREGISTER_CLIENT = 35;

    /**
     * 多端同步
     */
    public static final int SIDE_SYNC_CLIENT = 36;
    /**
     * 发送给另一端数据
     */
    public static final int SEND_OTHER_SIDE_MESSAGE = 37;

    /**
     * 500以后
     */
    public static final int SEND_MESSAGE_WS = 510;

    public static final int HEART_BEAT_WS = 511;
    
    public static final int UNREGISTER_CLIENT_WS = 535;




}
