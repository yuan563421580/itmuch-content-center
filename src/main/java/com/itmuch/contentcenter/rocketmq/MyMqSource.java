package com.itmuch.contentcenter.rocketmq;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

/**
 * 可以不用新建 但是为了保留之前的代码 新建一个
 */
public interface MyMqSource {

    String MY_MQ_OUTPUT = "my-mq-output";

    @Output(MY_MQ_OUTPUT)
    MessageChannel output();

}
