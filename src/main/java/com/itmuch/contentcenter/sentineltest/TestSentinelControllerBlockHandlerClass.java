package com.itmuch.contentcenter.sentineltest;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestSentinelControllerBlockHandlerClass {

    public static String block(String a, BlockException e) {
        log.warn("限流，或者降级了 block", e);
        return "限流，或者降级了 block";
    }

}
