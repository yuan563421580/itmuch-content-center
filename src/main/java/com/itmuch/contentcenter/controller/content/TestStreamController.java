package com.itmuch.contentcenter.controller.content;

import com.itmuch.contentcenter.rocketmq.MySource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring Cloud Stream 测试实现类
 */
@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TestStreamController {

    // Source 与 @EnableBinding(Source.class) 是一个
    private final Source source;

    // MySource 与 @EnableBinding(MySource.class) 是一个
    private final MySource mySource;

    /**
     * 生产消息
     * @return
     */
    @GetMapping("/test-stream")
    public String testStream() {
        this.source
                .output()
                .send(
                        MessageBuilder
                        .withPayload("test-stream 消息体")
                        .build()
                );
        return "success";
    }

    /**
     * 接口自定义 生产消息
     * @return
     */
    @GetMapping("/test-stream-my-source")
    public String testStreamMySource() {
        this.mySource
                .output()
                .send(
                        MessageBuilder
                        .withPayload("test-stream-my-source 消息体")
                        .build()
                );
        return "success";
    }



}
