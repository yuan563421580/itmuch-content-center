package com.itmuch.contentcenter.configuration;

import feign.Logger;
import org.springframework.context.annotation.Bean;

/**
 * feign的配置类
 * 这个类别加 @Configuration 注解了，否则必须挪到@ComponentScan能扫描的包以外
 *
 * 用于测试全局配置
 *    需要将原有的细粒度配置注释掉（UserCenterFeignClient 和 application.yml）
 *    在启动类Application上配置@EnableFeignClients(defaultConfiguration = GlobalFeignConfiguration.class)
 */
public class GlobalFeignConfiguration {

    @Bean
    public Logger.Level level(){
        // 让feign打印所有请求的细节
        return Logger.Level.FULL;
    }

}
