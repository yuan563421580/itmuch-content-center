package com.itmuch.contentcenter.sentineltest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestSentinelControllerFallBackClass {

    /**
     * 处理降级
     */
    public String fallback(String a, Throwable e) {
        log.warn("限流，或者降级了 fallback", e);
        return "限流，或者降级了 fallback";
    }

}
