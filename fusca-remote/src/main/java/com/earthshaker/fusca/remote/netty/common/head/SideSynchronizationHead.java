package com.earthshaker.fusca.remote.netty.common.head;

import java.util.List;

/**
 * @Author: zhubo
 * @Description 多端同步
 * @Date: 2021/7/12 5:16 下午
 */

public class SideSynchronizationHead extends WebSocketHeadPackHead {
    /**
     * 不需要同步的端
     */
    private List<String> excludeClientIds;

    public List<String> getExcludeClientIds() {
        return excludeClientIds;
    }

    public void setExcludeClientIds(List<String> excludeClientIds) {
        this.excludeClientIds = excludeClientIds;
    }
}
