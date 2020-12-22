package com.itmuch.contentcenter.configuration;

import com.itmuch.contentcenter.configuration.ribbon.RibbonConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Configuration;

/**
 * ribbon 全局配置 java 实现配置类
 *   方式：@RibbonClients(defaultConfiguration = RibbonConfiguration.class)
 *
 * 注意：测试的时候需要将 application.yml 中服务的细粒度配置注释掉
 */
@Configuration
@RibbonClients(defaultConfiguration = RibbonConfiguration.class)
public class DefaultRibbonConfiguration {


}
