package com.itmuch.contentcenter.configuration;

import com.itmuch.contentcenter.configuration.ribbon.RibbonConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Configuration;

/**
 * user-center 服务 ribbon 细粒度配置
 * java 实现配置类
 *
 * 细粒度属性配置方式 进行 测试
 *      将 @Configuration 和 @RibbonClient 注释掉
 *
 * 实际建议使用配置属性方式
 */
//@Configuration
//@RibbonClient(name = "user-center", configuration = RibbonConfiguration.class)
public class UserCenterRibbonConfiguration {
}
