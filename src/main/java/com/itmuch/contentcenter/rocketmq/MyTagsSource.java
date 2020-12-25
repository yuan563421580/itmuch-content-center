package com.itmuch.contentcenter.rocketmq;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface MyTagsSource {

    String MY_TAGS_OUTPUT = "my-tags-output";

    @Output(MY_TAGS_OUTPUT)
    MessageChannel output();

}
