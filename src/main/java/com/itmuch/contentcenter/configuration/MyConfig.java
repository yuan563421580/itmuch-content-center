package com.itmuch.contentcenter.configuration;

import com.itmuch.contentcenter.annotation.ScanIgnore;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ScanIgnore
public class MyConfig {

    /**
     * 说明：本类是测试类
     * 通过 @ScanIgnore 实现启动类 Application 同包下的配置类不被扫描
     * 可以用来查看加载顺序及文件 需要注释掉@ScanIgnore
     *
     * 启动类上添加
     * @ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ScanIgnore.class))
     *
     * 与 @ComponentScan(excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {MyConfig.class})})
     * 对比 好处:
     *
     * 多个排除类时候不需要修改 Application 启动类，同时 Application 启动类不会有太多这种配置
     */

    @Bean
    public BeanPostProcessor beanPostProcessor() {
        System.out.println("初始化了 bean BeanPostProcessor");
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                System.out.println("加载了bean " + beanName);
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                return bean;
            }
        };
    }

}
