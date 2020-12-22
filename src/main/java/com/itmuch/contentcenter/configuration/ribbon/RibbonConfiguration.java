package com.itmuch.contentcenter.configuration.ribbon;

import com.itmuch.contentcenter.annotation.ScanIgnore;
import com.itmuch.contentcenter.configuration.NacosFinalRule;
import com.itmuch.contentcenter.configuration.NacosSameClusterWeightedRule;
import com.itmuch.contentcenter.configuration.NacosWeightedRule;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.PingUrl;
import com.netflix.loadbalancer.RandomRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RibbonConfiguration 类上有 @Configuration 注解
 * @Configuration 注解上有 @Component
 * Configuration 是一种特殊的 Component
 * -
 * 启动类上存在 @SpringBootApplication 注解，本质上是一个组合注解
 * 其中包含 @ComponentScan 是用来扫描 Component 的 ，默认扫描位置是当期启动类所在的包和包下所有的Component
 * -
 * Ribbon 的配置类不能被扫描到，因为spring的上下文是树状的上下文
 * 在应用中，applicationContext , 也就是spring boot的扫描的上下文就主上下文
 * ribbon也会有一个上下文，是子上下文
 * 父子上下文扫描的包如果重叠会产生各种问题
 * -
 * 如果RibbonConfiguration被@ComponentScan重复扫描，会被所有的@RibbonClient共享
 *
 * *重要*
 * 保证 RibbonConfiguration 不被重复扫描，
 *  方式1：自定义@ScanIgnore实现，RibbonConfiguration上以使用 @ScanIgnore 自定义注解，
 *      启动类增加@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ScanIgnore.class))
 *  方式2：RibbonConfiguration 所在的包移到启动类所在包评级目录
 *      如创建com.itmuch.ribbonconfiguration文件夹存放
 * -
 * 父子上下文扫描问题导致的经典问题：
 * Spring+SpringMVC 配置事务管理无效原因及解决方案。
 * https://blog.csdn.net/qq_32588349/article/details/52097943
 * 结论：问题主要在于SpringMVC的配置文件扫包范围，Spring的配置文件就算也扫了@Controller注解，
 *      但是在SpringMVC会重新扫描一次，事务管理的Service只要没被重新扫描就不会出现事务失效问题。
 */

@Configuration
@ScanIgnore
public class RibbonConfiguration {

    /**
     * 查找接口实现类的快捷键 : ctrl + alt +B
     * 查看类或接口的继承关系 : ctrl + h
     */

    @Bean
    public IRule ribbonRule() {
        // 随机的规则
        //return new RandomRule();

        // 支持权重的自定义实现规则
        //return new NacosWeightedRule();

        // 支持同集群权重的自定义实现规则
        //return new NacosSameClusterWeightedRule();

        // 支持基于元数据的版本 和 同集群下的负载均衡
        return new NacosFinalRule();
    }

    /*@Bean
    public IPing ping() {
        return new PingUrl();
    }*/

}
