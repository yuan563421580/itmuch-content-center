package com.itmuch.contentcenter.configuration;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.ribbon.NacosServer;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.core.Balancer;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 获取相同集群的负载均衡
 */
@Slf4j
public class NacosSameClusterWeightedRule extends AbstractLoadBalancerRule {

    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        // 读取配置文件，并初始化NacosSameClusterWeightedRule
    }

    @Override
    public Server choose(Object key) {
        // 1.找到指定服务的所有实例 A
        // 2.过滤出相同集群下的所有实例 B
        // 3.如果B是空，就用A
        // 4.基于权重的负载均衡算法，返回1个实例

        try {
            // 获取配置文件中的集群名称 BJ
            // spring.cloud.nacos.discovery.cluster-name: BJ
            String clusterName = nacosDiscoveryProperties.getClusterName();

            // 实现负载均衡算法 ...
            // 基于NacosClient的负载均衡算法实现

            // LoadBalancer是Ribbon的入口 ；强转成BaseLoadBalancer
            // ILoadBalancer loadBalancer = this.getLoadBalancer();
            BaseLoadBalancer loadBalancer = (BaseLoadBalancer) this.getLoadBalancer();

            // 想要请求的微服务的名称
            String name = loadBalancer.getName();

            // 获取服务发现相关的API
            NamingService namingService = nacosDiscoveryProperties.namingServiceInstance();

            // 1.找到指定服务的所有实例 A (健康的实例)
            List<Instance> instances = namingService.selectInstances(name, true);

            // 2.过滤出相同集群下的所有实例 B
            List<Instance> sameClusterInstances = instances.stream()
                    .filter(instance -> Objects.equals(instance.getClusterName(), clusterName))
                    .collect(Collectors.toList());

            // 3.如果B是空，就用A
            List<Instance> instancesToBeChosen = new ArrayList<>();
            if (CollectionUtils.isEmpty(sameClusterInstances)) {
                instancesToBeChosen = instances;
                log.warn("发生跨集群的调用，name = {}，clusterName = {}， instance = {}",
                        name, clusterName, instances);
            } else {
                instancesToBeChosen = sameClusterInstances;
            }

            // 4.基于权重的负载均衡算法，返回1个实例
            /*
             *   基于NacosWeightedRule测试类 ： NacosWeightedRule 查找出这个算法
             *   nacos client 自动通过基于权重的负载均衡算法，选择一个实例
             *      Instance instance = namingService.selectOneHealthyInstance(name);
             *   查找接口实现类的快捷键 : ctrl + alt +B
             *   NacosNamingService.class : RandomByWeight.selectHost(...)
             *       -> Balancer.class : selectHost()
             *       -> Balancer.class : Instance getHostByRandomWeight(List<Instance> hosts)
             */
            Instance instance = ExtendBalancer.getHostByRandomWeight2(instancesToBeChosen);
            log.info("选择的实例是 port = {}, instance = {}", instance.getPort(), instance);

            return new NacosServer(instance);
        } catch (NacosException e) {
            log.error("发生异常了", e);
            return null;
        }
    }
}


/**
 * Instance getHostByRandomWeight 是 protected
 * 简单进行包装改成 public 的，这样可以实现调用
 */
class ExtendBalancer extends Balancer {
    public static Instance getHostByRandomWeight2(List<Instance> hosts) {
        return getHostByRandomWeight(hosts);
    }
}
