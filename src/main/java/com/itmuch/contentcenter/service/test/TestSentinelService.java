package com.itmuch.contentcenter.service.test;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TestSentinelService {

    @SentinelResource("common")
    public String testCommon() {
        log.info("common...");
        return "common";
    }

}
