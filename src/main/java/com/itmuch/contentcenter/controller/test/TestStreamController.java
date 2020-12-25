package com.itmuch.contentcenter.controller.test;

import com.itmuch.contentcenter.rocketmq.MySource;
import com.itmuch.contentcenter.rocketmq.MyTagsSource;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
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

    // MyTagsSource 与 @EnableBinding(MyTagsSource.class) 是一个
    private final MyTagsSource myTagsSource;

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

    /**
     * 由于条件过滤测试编写 : Condition 方式实现 :
     * 需要消费者 @StreamListener(value = Sink.INPUT, condition = "headers['my-header']=='my-condition-header'")
     * @return
     */
    @GetMapping("/test-stream-condition")
    public String testStreamCondition() {
        this.source
                .output()
                .send(
                        MessageBuilder
                                .withPayload("test-stream-condition 消息体")
                                // 消息过滤 condition 方式设定 header
                                .setHeader("my-header", "my-condition-header")
                                .build()
                );
        return "success";
    }

    /**
     * 由于条件过滤测试编写 : Tags 方式实现 : 只能设置1个tag
     * @return
     */
    @GetMapping("/test-stream-condition-tags")
    public String testStreamConditionTags() {
        this.myTagsSource
                .output()
                .send(
                        MessageBuilder
                                .withPayload("test-stream-condition-tags 消息体")
                                // 注意：只能设置1个tag
                                .setHeader(RocketMQHeaders.TAGS, "tag1")
                                .build()
                );
        return "success";
    }



}
