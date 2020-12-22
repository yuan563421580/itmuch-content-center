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
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.loadbalancer.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 支持基于元数据的版本 和 同集群下的负载均衡
 *
 * 我们需要实现的有两点：
 *      优先选择同集群下，符合metadata的实例
 *      如果同集群加没有符合metadata的实例，就选择所有集群下，符合metadata的实例
 */
@Slf4j
public class NacosFinalRule extends AbstractLoadBalancerRule {

    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        // 读取配置文件，并初始化NacosFinalRule
    }

    @Override
    public Server choose(Object key) {
        // 负载均衡规则：优先选择同集群下，符合metadata的实例
        // 如果没有，就选择所有集群下，符合metadata的实例

        // 1. 查询所有实例 A
        // 2. 筛选元数据匹配的实例 B
        // 3. 筛选出同cluster下元数据匹配的实例 C
        // 4. 如果C为空，就用B
        // 5. 随机选择实例
        try {
            // 获取配置文件中的集群名称
            String clusterName = nacosDiscoveryProperties.getClusterName();
            // 获取元数据信息
            Map<String, String> metadata = nacosDiscoveryProperties.getMetadata();
            // 允许调用实例的版本
            String targetVersion = MapUtils.getString(metadata, "target-version", "");

            // LoadBalancer是Ribbon的入口 ；强转成DynamicServerListLoadBalancer
            DynamicServerListLoadBalancer loadBalancer = (DynamicServerListLoadBalancer) this.getLoadBalancer();

            // 想要请求的微服务的名称
            String name = loadBalancer.getName();

            // 获取服务发现相关的API
            NamingService namingService = nacosDiscoveryProperties.namingServiceInstance();

            // 获取要调用微服务下的所有健康的实例
            List<Instance> instances = namingService.selectInstances(name, true);

            List<Instance> metadataMatchInstances = instances;
            // 如果配置了版本映射，那么只调用元数据匹配的实例
            if (StringUtils.isNotBlank(targetVersion)) {
                // 判断允许调用实例的版本 是否与 当前实例的版本 一致
                metadataMatchInstances = instances.stream()
                        .filter(instance -> Objects.equals(targetVersion, MapUtils.getString(instance.getMetadata(), "version")))
                        .collect(Collectors.toList());
                if (CollectionUtils.isEmpty(metadataMatchInstances)) {
                    log.warn("未找到元数据匹配的目标实例！请检查配置。targetVersion = {}, instance = {}", targetVersion, instances);
                    return null;
                }
            }

            List<Instance> clusterMetadataMatchInstances = metadataMatchInstances;
            // 如果配置了集群名称，需筛选同集群下元数据匹配的实例
            if (StringUtils.isNotBlank(clusterName)) {
                clusterMetadataMatchInstances = metadataMatchInstances.stream()
                        .filter(instance -> Objects.equals(instance.getClusterName(), clusterName))
                        .collect(Collectors.toList());
                if (CollectionUtils.isEmpty(clusterMetadataMatchInstances)) {
                    clusterMetadataMatchInstances = metadataMatchInstances;
                    log.warn("发生跨集群调用。clusterName = {}, targetVersion = {}, clusterMetadataMatchInstances = {}",
                            clusterName, targetVersion, clusterMetadataMatchInstances);
                }
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
            Instance instance = ExtendBalancer1.getHostByRandomWeight2(clusterMetadataMatchInstances);
            log.info("选择的实例是 port = {}, instance = {}", instance.getPort(), instance);

            return new NacosServer(instance);
        } catch (Exception e) {
            return null;
        }
    }
}


class ExtendBalancer1 extends Balancer{
    public static Instance getHostByRandomWeight2(List<Instance> hosts) {
        return getHostByRandomWeight(hosts);
    }
}
