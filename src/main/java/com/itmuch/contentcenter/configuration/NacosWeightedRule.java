package com.itmuch.contentcenter.configuration;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.ribbon.NacosServer;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 权重支持实现配置类
 *  实现IRule接口：implements IRule
 *  为了更加方便的扩展Ribbon， Ribbon还提供了一个抽象类：AbstractLoadBalancerRule
 *
 * 测试：将Nacos的80主机权重改成0，
 *      测试进入现在都返回的结果：选择的实例：port = 8081, instance = {}
 *
 * Nacos的负载均衡策略没有被Spring Cloud包含的原因
 *  spring cloud commons --> 定义了标准
 *  子项目 spring cloud loadbalancer ---> 定义了负载均衡器的标准，没有权重的概念
 *  所以 spring cloud alibaba 遵循了标准，整合了ribbon
 */
@Slf4j
public class NacosWeightedRule extends AbstractLoadBalancerRule {

    @Autowired
    private NacosDiscoveryProperties discoveryProperties;

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        // 读取配置文件，并初始化NacosWeightedRule
    }

    @Override
    public Server choose(Object key) {
        try {
            // LoadBalancer是Ribbon的入口 ；强转成BaseLoadBalancer
            // ILoadBalancer loadBalancer = this.getLoadBalancer();
            BaseLoadBalancer loadBalancer = (BaseLoadBalancer) this.getLoadBalancer();

            // 想要请求的微服务的名称
            String name = loadBalancer.getName();

            // 实现负载均衡算法 ...
            // 基于NacosClient的负载均衡算法实现

            // 取到服务发现相关的API
            NamingService namingService = discoveryProperties.namingServiceInstance();

            // nacos client 自动通过基于权重的负载均衡算法，选择一个实例
            Instance instance = namingService.selectOneHealthyInstance(name);
            log.info("选择的实例：port = {}, instance = {}", instance.getPort(), instance);

            // instance 转换成 server 返回
            return new NacosServer(instance);
        } catch (NacosException e) {
            e.printStackTrace();
            return null;
        }
    }
}
