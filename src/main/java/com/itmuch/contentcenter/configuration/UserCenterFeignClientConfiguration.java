package com.itmuch.contentcenter.configuration;

import feign.Logger;
import org.springframework.context.annotation.Bean;

/**
 * feign的配置类
 * 这个类别加 @Configuration 注解了，否则必须挪到@ComponentScan能扫描的包以外
 */
public class UserCenterFeignClientConfiguration {

    @Bean
    public Logger.Level level(){
        // 让feign打印所有请求的细节
        return Logger.Level.FULL;
    }

}
