package com.earthshaker.fusca.remote;


import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @Author: zhubo
 * @Description
 * @Date: 2021/6/30 8:57 下午
 */
public class SocketAddressUtil {

    public static SocketAddress string2SocketAddress(final String addr) {
        int split = addr.lastIndexOf(":");
        String host = addr.substring(0, split);
        String port = addr.substring(split + 1);
        InetSocketAddress isa = new InetSocketAddress(host, Integer.parseInt(port));
        return isa;
    }

    public static String parseRemoteAddr(final SocketAddress remote ) {
        if (null == remote) {
            return "";
        }
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }

        return "";
    }

    public static String parseSocketAddressAddr(SocketAddress socketAddress) {
        if (socketAddress != null) {
            final String addr = socketAddress.toString();

            if (addr.length() > 0) {
                return addr.substring(1);
            }
        }
        return "";
    }
}
