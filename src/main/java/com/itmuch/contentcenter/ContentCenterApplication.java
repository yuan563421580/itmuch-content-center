package com.itmuch.contentcenter;

import com.alibaba.cloud.sentinel.annotation.SentinelRestTemplate;
import com.itmuch.contentcenter.annotation.ScanIgnore;
import com.itmuch.contentcenter.configuration.GlobalFeignConfiguration;
import com.itmuch.contentcenter.configuration.MyConfig;
import com.itmuch.contentcenter.interceptor.RestTemplateTokenRelayInterceptor;
import com.itmuch.contentcenter.rocketmq.MyMqSource;
import com.itmuch.contentcenter.rocketmq.MySource;
import com.itmuch.contentcenter.rocketmq.MyTagsSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.client.RestTemplate;
import tk.mybatis.spring.annotation.MapperScan;

import java.util.Collections;

/**
 * @MapperScan("com.itmuch") 扫描 mybatis 里面的包的接口
 * @EnableDiscoveryClient 通过 Spring Cloud 原生注解 @EnableDiscoveryClient 开启服务注册发现功能 , Edgware版本开始可以不使用该注释
 * @EnableFeignClients 开启 Feign 的支撑
 * @EnableFeignClients(defaultConfiguration = GlobalFeignConfiguration.class) 开启 Feign 的支撑同时配置全局配置测试
 * @EnableBinding(Source.class) 实现 Spring Cloud Stream 【发送】消息注解
 */
//@ComponentScan(excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {MyConfig.class})})
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ScanIgnore.class))
@MapperScan("com.itmuch.contentcenter.dao")
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients//(defaultConfiguration = GlobalFeignConfiguration.class)
@EnableBinding({Source.class, MySource.class, MyMqSource.class, MyTagsSource.class})
public class ContentCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentCenterApplication.class, args);
    }

    /**
     * 在spring容器中，创建一个对象，类型RestTemplate；名称/ID是：restTemplate
     *  <bean id="restTemplate" class="xxx.RestTemplate"/>
     * @LoadBalanced 注解的作用 : 为  RestTemplate 整合 Ribbon
     */
    @Bean
    @LoadBalanced
    @SentinelRestTemplate
    public RestTemplate restTemplate() {
        //return new RestTemplate();

        RestTemplate restTemplate = new RestTemplate();

        // 实现拦截器配置
        restTemplate.setInterceptors(
                Collections.singletonList(
                        new RestTemplateTokenRelayInterceptor()
                )
        );
        return restTemplate;
    }

    /**
     * Nacos 控制台访问地址：http://39.102.66.189:8848/nacos
     * Sentinel 控制台访问地址：http://39.102.66.189:8849/#/login
     *                       http://localhost:8849/#/login
     * RocketMQ 控制台访问地址：http://39.102.66.189:17890/
     */

    /**
     * 整合 MyBatis
     *  · pom.xml 引入依赖 mysql-connector-java 、 mapper-spring-boot-starter
     *  · Application 启动类上使用注解 @MapperScan("com.itmuch") 扫描 mybatis 里面的包的接口
     *
     * MyBatis通用Mapper （自动代码生成）
     *  https://github.com/abel533/Mapper
     *  ·  /resources/generator/generatorConfig.xml 和 /resources/generator/config.properties
     *  ·  生成的实体类需要手动添加lombok注解： @Data 、 @Builder 、 @NoArgsConstructor 、 @AllArgsConstructor
     *      因为现在自动生成器不支持生成这几个注解
     *
     * 简单说明一下用法比较
     *  ·  updateByPrimaryKeySelective 会对字段进行判断再更新(如果为Null就忽略更新)，如果你只想更新某一字段，可以用这个方法。
     *  ·  updateByPrimaryKey 对你注入的字段全部更新
     */

    /**
     * 整合 Nacos
     * ~ pom.xml 引入依赖 spring-cloud-starter-alibaba-nacos-discovery
     * ~ Application 上使用注解 @EnableDiscoveryClient 开启服务注册发现功能
     * ~ application.yml 中添加配置 spring.cloud.nacos.discovery.server-addr=服务地址
     *
     * 服务发现的领域模型
     * · Namespace : 实现隔离，默认public
     * · Group : 不同服务可以分到一个组，默认DEFAULT_GROUP (现在实际还没有支撑)
     * · Service : 微服务
     * · Cluster : 对指定微服务的一个虚拟划分，默认DEFAULT
     * · Instance : 微服务实例
     *
     * 具体实际整合应用需要根据 Ribbon 相关知识点继续深入学习
     */

    /**
     * Ribbon接口（名称：作用：默认值）：
     * · IClientConfig ：读取配置：DefaultClientConfigImpl
     * · IRule ：负载均衡规则，选择实例：ZoneAvoidanceRule
     * · IPing ：筛选掉ping不通的实例：DummyPing
     * · ServerList<Server> ：交给Ribbon的实例列表：Ribbon : ConfigurationBasedServerList
     * 	                                        Spring Cloud Alibaba : NacosServerList
     * · ServerListFilter<Server> ：过滤掉不符合条件的实例 ：ZonePreferenceServerListFilter
     * · ILoadBalancer : Ribbon的入口 ：ZoneAwareLoadBalancer
     * · ServerListUpdater : 更新交给Ribbon的List的策略 ：PollingServerListUpdater
     *
     * ---
     *
     * Ribbon规则：
     * · AvailabilityFilteringRule : 过滤掉一直连接失败的被标记为circuit tripped的后端Server，
     *      并过滤掉那些高并发的后端Server或者使用一个AvailabilityPredicate来包含过滤server的逻辑，
     *      其实就是检查status里记录的各个Server的运行状态
     * · BestAvailableRule ：选择一个小的并发请求的Server，逐个考察Server，如果Server被tripped，则跳过
     * · RandomRule : 随机选择一个Server
     * · ResponseTimeWeightedRule : 已废弃，作用同WeightedResponseTimeRule
     * · RetryRule : 对选定的负载均衡策略机上重试机制，在一个配置时间段内当选择Server不成功，
     *      则一直尝试使用subRule的方式选择一个可用的Server
     * · RoundRobinRule : 轮询选择，轮询index，选择index对应位置的Server
     * · WeightResponseTimeRule : 根据响应时间加权，响应时间越长，权重越小，呗选中的可能性越低
     * · ZoneAvoidanceRule : 复合判断Server所Zone的性能和Server的可用性选择Server，
     *      在没有Zone的环境下，类试与轮询（RoundRobinRule）
     *
     * ---
     *
     * Ribbon实现（user-center）细粒度配置（实际建议使用配置属性方式）：
     * · java代码实现（进入代码阅读注意细节）：
     *      主要包括： UserCenterRibbonConfiguration 和 RibbonConfiguration
     * · 配置属性方式：在application.xml配置
     *      <clientName>.ribbon.NFLoadBalancerRuleClassName=响应规则的全路径
     *      -
     *      user-center:
     *          ribbon:
     *              NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule
     * ···
     * 细粒度配置对比及实践：
     *  · 代码配置：优点：基于代码，更加灵活。
     *            缺点：有小坑（父子上下文）；线上修改需重新打包、发布。
     *  · 属性配置：优点：易上手；配置更加直观；线上修改无需重新打包、发布；优先级更高。
     *            缺点：极端场景下没有代码配置方式灵活。
     *  · 最佳实践：
     *      ~ 尽量使用属性配置，属性方式实现不了的情况下在考虑用代码配置。
     *      ~ 在同一个微服务内尽量保持单一性，比如统一属性配置，不要两种方式混用，增加定位代码的复杂性。
     *
     * ---
     *
     * Ribbon实现全局配置：
     *  · 方式一（强烈不建议使用）：让ComponentScan上下文重叠
     *  · 方式二（唯一正确的途径）：@RibbonClients(defaultConfiguration=xxx.class)
     *  · java代码方式实现：
     *      主要包括： DefaultRibbonConfiguration 和 RibbonConfiguration
     *  · 配置属性方式实现：
     *      <clientName>.ribbon.如下属性：
     * 	        NFLoadBalancerClassName : ILoadBalancer 实现类
     * 	        NFLoadBalancerRuleClassName : IRule 实现类
     * 	        NFLoadBalancerPingClassName : IPing 实现类
     * 	        NIWSServerListClassName : ServerList 实现类
     * 	        NIWSServerListFilterClassName : ServerListFilter 实现类
     *
     * ---
     *
     * 扩展Ribbon - 权重支持
     *  · Nacos界面配置权重：服务列表 -> 具体的服务(多个实例) -> 详情 -> 编辑 -> 输入【权重】（0-1） -> 保存
     *      权重越大，调用的机会越大。可以用于部分性能不好的主机少分发调用。
     *  · java代码方式实现：NacosWeightedRule 和 RibbonConfiguration
     *      RibbonConfiguration 中 IRule 方法返回自定义的负载均衡算法
     *  · 手记：扩展Ribbon支持Nacos权重的三种方式：https://www.imooc.com/article/288660
     * ···
     * 扩展Ribbon - 同集群优先
     *  · java代码方式实现：NacosSameClusterWeightedRule 和 RibbonConfiguration
     *      需要在application.yml文件中同时配置cluster-name
     *      进行测试的时候，需要将user-center的配置文件分别配置cluster-name为BJ和NJ进行测试，观察调用
     * ···
     * 扩展Ribbon - 基于元数据的版本控制
     *  · java代码方式实现：NacosFinalRule 和 RibbonConfiguration
     *      需要在application.yml文件中同时配置metadata 、cluster-name
     *      进行测试的时候，需要将user-center的配置文件分别配置metadata 、cluster-name，观察调用
     *  · 手记：https://www.imooc.com/article/288674
     * ···
     * Nacos 通过 namespace 实现隔离
     *    测试过程：
     *      ~ 先将user-center开启配置namespace，
     *          观察Nacos：服务列表 在 dev ；调用报错：No instances available for user-center
     *      ~ 再将content-center开启配置namespace，
     *          观察Nacos：服务列表 在 dev ；调用成功
     *
     * ---
     *
     * 使用Ribbon有几个缺点：
     *  1、代码不可读 ；2、复杂的url难以维护
     *  3、难以响应需求的变化 ；4、编程体验不统一
     */

    /**
     * Feign 是基于 Ribbon 的，所以在 Ribbon 中配置的负载均衡策略可以继续支撑
     *
     * Feign 是 Netflix 开源的声明式 HTTP 客户端
     *
     * ---
     *
     * Feign 的组成（接口 : 作用 : 默认值）：
     *  · Feign.Builder : Feign的入口 : Feign.Builder
     *  ·  Client : Feign底层用什么去请求 : 和 Ribbon 配合时 LoadBalancerFeignClient
     * 								 不和 Ribbon 配合时 feign.Client.Default
     *  ·  Contract : 契约，注解支持 : SpringMvcContract
     *  ·  Encoder : 编码器，用于将对象转换成HTTP请求消息体 : SpringEncoder
     *  ·  Decoder : 解码器，将响应消息体转换成对象 : ResponseEntityDecoder
     *  ·  Logger : 日志管理器 : Slf4jLogger
     *  ·  RequestInterceptor : 用于为每个请求添加通用逻辑 : 无
     *
     * ---
     *
     * Feign日志级别（级别 : 打印内容）
     *  ·  NONE(默认值) : 不记录任何记录
     *  ·  BASIC : 仅记录请求方法、URL、响应状态代码及执行时间
     *  ·  HEADERS : 记录BASIC级别基础上，记录请求和响应的header
     *  ·  FULL : 记录请求和响应的header、body和元数据
     *
     * 自定义Feign日志级别
     *  细粒度配置 - 实现日志配置：
     *  · java 代码方式：
     *      ~ 创建 UserCenterFeignClientConfiguration
     *      ~ @FeignClient(name = "user-center", configuration = UserCenterFeignClientConfiguration.class)
     *      ~ application.yml 配置 ：logging.level.<feignName全路径>: debug
     *  · 属性配置方式：
     *      ~ application.yml 配置 ：feign.client.config.<feignName>.loggerLevel: full
     *  ...
     *  全局配置 - 实现日志配置：
     *  · java 代码方式：
     *      方式一：让父子上下文ComponentScan重叠（强烈不建议使用）
     *      方式二：@EnableFeignClients(defaultConfiguration=xxx.class)【唯一正确的途径】
     *          详请见：GlobalFeignConfiguration 中实现讲解
     *  · 属性配置方式：
     *      ~ application.yml 配置 ：feign.client.config.default.loggerLevel: full
     *  ...
     *  注意：日志配置，一定要开启logging.level , 否者不会打印
     *
     * 支持的配置型
     *  · 代码方式（配置项 : 作用）
     *      ~ Feign.Bulider : Feign的入口
     *      ~ Client : Feign底层用什么去请求
     *      ~ Contract : 契约，注解支持
     *      ~ Encoder : 编码器，用于将对象转换成HTTP请求消息体
     *      ~ Decoder : 解码器，将响应消息体转换成对象
     *      ~ Logger : 日志管理器
     *      ~ Logger.Level : 指定日志级别
     *      ~ Retryer : 指定重试策略
     *      ~ ErrorDecoder : 指定错误解码器
     *      ~ Request.Options : 超时时间
     *      ~ Collection<RequestInteeceptor> : 拦截器
     *      ~ SetterFactory : 用于设置Hystrix的配置属性，Feign整合Hystrix才会用
     *  · 属性方式
     *      feign.client.config:
     * 	        <feignName>:
     * 		        connectTimeout: 5000 # 连接超时时间
     * 		        readTimeout: 5000 	 # 读取超时时间
     * 		        loggerLevel: full  	 # 日志级别
     * 		        errorDecoder: com.example.SimpleErrorDecoder # 错误解码器
     * 		        retryer: com.example.SimpleRetryer			 # 重试策略
     * 		        requestInterceptors:
     * 			        - com.example.FooRequestInterceptor		 # 拦截器
     * 		        # 是否对404错误码解码
     * 		        # 处理逻辑详见feign.SynchronousMethodHandler#executeAndDecode
     * 		        decode404: false
     * 		        encoder: com.example.SimpleEncoder   # 编码器
     * 		        decoder: com.example.SimpleDecoder   # 解码器
     * 		        contract: com.example.SimpleContract # 契约
     *
     * ---
     *
     * Feign 的继承特性：
     *     可以学习使用一下，但是官方不建议使用，因为带来了紧耦合。
     *     但是业界的现状很多公司有使用，是一种契约。对于代码就是继承。
     *
     * 使用 Feign 构造多参数的请求
     *      代码在：TestUserCenterFeignClient 和 TestController
     *      https://www.imooc.com/article/289000
     *
     * Feign 和 RestTemplate 如何选择：
     *      原则：尽量使用Feign，杜绝使用RestTemplate。事无绝对，合理选择。
     *
     * Feign 的性能优化的两个步骤：
     *  （知识点：默认情况下Feign使用URLConnection连接方式，没有连接池。
     *          Feign底层还支持Apache的HttpClient和OkHttpClient。HttpClient和OkHttpClient支持连接池）
     *   · 01).连接池：性能提升15%左右 | HttpClient为例实现：
     *      ~ 添加依赖：feign-httpclient
     *      ~ yml文件修改配置：feign.httpclient.enabled=true 、设置max-connections 和 max-connections-per-route
     *   · 02).设置合适的日志级别，生产环境建议将日志级别设置成basic ，开发环境可以测试成full用于观察学习
     *
     * Feign常见问题总结：http://www.imooc.com/article/289005
     *
     */

    /**
     * 雪崩效应，也就级联失效或级联故障
     *
     * 常见容错方案：
     *  · 超时：设置超时时间，超过这个时间调用的线程就会被释放
     *  · 限流：高并发的系统才可能会存在线程阻塞，根据每个微服务最大承载的QPS数目设定，某个服务达到阈值，有流量进入会直接拒绝
     *  · 仓壁模式：微服务A的某个Controller调用微服务B ；将微服务A设定独立的【线程池】，当服务B挂掉了，
     *                  服务A过一会线程就满了，然后去排队，再然后就直接拒绝了。
     *            微服务A的其他Controller也有自己的【线程池】，不影响其他Controller继续调用。
     *  · 断路器模式：
     *      ~ 白话理解：家里的电闸，实时的监控电路的状态，当发现某段时间内电流过大，就认为电路短路，就会跳闸，从而保证电路不被烧毁。
     *      ~ 软件中断路器理解：假设监测某个API的调用，5秒之内的错误率、错误次数等，如果错误率或者错误次数达到某一个设定的阈值，
     *            就认为这里面的代码所依赖的服务是挂掉了的，达到了阈值就不去调用远程的API了（相当于跳闸）直接返回错误。
     *      ~《断路器模式》原文：https://martinfowler.com/bliki/CircuitBreaker.html
     *
     * Sentinel 是什么？
     *   随着微服务的流行，服务和服务之间的稳定性变得越来越重要。
     *   Sentinel 以流量为切入点，从流量控制、熔断降级、系统负载保护等多个维度保护服务的稳定性。
     *   简答：轻量级的流量控制、熔断降级 Java 库。
     *
     * 整合 Sentinel
     *   ~ pom.xml 中引入依赖 spring-cloud-starter-alibaba-sentinel
     *   ~ 添加 Sentinel 会暴露 /actuator/sentinel ，所以需要引入依赖 spring-boot-starter-actuator
     *      而 Springboot 默认是没有暴露该端点的，所以我们需要自己在 yml 中设置 management.endpoints.web.exposure.include='*'
     *   ~ 访问：http://localhost:8010/actuator/sentinel
     * 连接 Sentinel 控制台
     *   ~ application.yml 中添加配置 spring.cloud.sentinel.transport.dashboard=服务地址
     * 特殊说明：
     *    01).Sentinel 是懒加载的，可以先调用在查看
     *    02).springboot 使用 spring-cloud-starter-alibaba-sentinel 导致响应变成xml格式
     *        是因为引入 sentinel 依赖会同时引入 jackson-dataformat-xml。xml优先级比json高。
     *        在 sentinel 依赖排除 jackson-dataformat-xml 依赖即可正常展示 json 格式。
     *    03).测试时候需要保证sentinel和本地微服务可以进行通信，所以在本地启动进行测试。
     * 遇到问题：
     *    01).本次测试从最开始的时候将 sentinel 部署到了阿里云服务器上，服务也能正常注册到控制台。 但是控制台取数据失败了。
     *        错误描述：Failed to fetch metric from <http://192.168.242.116:8719/metric?startTime=1608273232000
     *                  &endTime=1608273238000&refetch=false> (ConnectionException: Connection timed out)
     *        错误分析：控制台所在的服务器必须能够访问192.168.242.116，这样控制台才能获得微服务的监控信息、将规则推送给微服务。
     *                通常情况下服务器是没法访问本地的，那么这种情况只能将微服务也部署到能够互相通信的服务器上才能继续。
     *                也可以重写sentinel的transport模块，改变现有的通信机制，不过这种方案成本非常高。
     *                部分论坛说可以配置客户端IP: sentinel.transport.client-ip 解决，实际测试不能解决该问题（应该可做生产配置参考）。
     *
     * ---
     *
     * Sentinel 各种规则配置使用说明：
     * 规则参数总结：https://www.imooc.com/article/289345
     *  · 限流规则：
     *      ~ 流控模式【直接】（默认）：就是只关联当前的API的QPS和线程数是否达到阈值。
     *      ~ 流控模式【关联】：当关联的资源达到阈值，就限流自己
     *          一般是保护关联资源的一种模式，当关联资源达到阈值，请求资源就会返回失败。经常用于读写操作。保护写，防止读过快影响写。
     *      ~ 流控模式【链路】：只记录指定链路上的流量。
     *          测试：/test-a 和 /test-b 主要是依赖 @SentinelResource("common")。在 sentinel 需要进行配置。
     *      【入坑】：实际测试 关联和链路各种配置不生效，查阅资料。
     *          从1.6.3 版本开始，Sentinel Web filter默认收敛所有URL的入口context，因此链路限流不生效。
     *          从1.7.0 版本开始（对应SCA的2.1.1.RELEASE)，官方在CommonFilter 引入了WEB_CONTEXT_UNIFY 参数，
     *              用于控制是否收敛context。将其配置为 false 即可根据不同的URL 进行链路限流。
     *          从SCA 2.1.1.RELEASE之后的版本,可以通过配置spring.cloud.sentinel.web-context-unify=false即可关闭收敛。
     *      【脱坑】：资料上描述两种解决方案（按方案1解决）
     *          方案1、引入 sentinel-web-servlet 依赖 后 创建 FilterContextConfig
     *          方案2、配置spring.cloud.sentinel.web-context-unify=false （因为版本原因，配置不生效）
     *
     *  · 配置流控效果
     *      ~ 快速失败（默认）: 直接失败，抛出异常，不做任何额外的处理，是最简单的效果
     *          - 源码 ： com.alibaba.csp.sentinel.slots.block.flow.controller.DefaultController
     *      ~ Warm Up：它从开始阈值到最大QPS阈值会有一个缓冲阶段，根据codeFactor(冷加载因子，默认3)，从阈值/codeFactor，
     *                  经过预热时长，才到达设置的QPS阈值。
     *                 适用于将突然增大的流量转换为缓步增长的场景，如某个API为抢购：只有抢购时间才会突然大量请求。
     *          - 源码 ： com.alibaba.csp.sentinel.slots.block.flow.WarmUpController
     *      ~ 排队等待：匀速排队，让请求以均匀的速度通过，单机阈值为每秒通过数量，其余的排队等待；
     *                  它还会让设置一个超时时间，当请求超过超时间时间还未处理，则会被丢弃。
     *                排队等待只能设置QPS，设置线程是无效的。
     *                适用于应对突发流量的场景，比如：突然来了好多请求，过一会就空闲，可以在空闲的时候处理没有处理完的请求，而不是直接拒绝。
     *          - 源码 ： com.alibaba.csp.sentinel.slots.block.flow.RateLimterController
     *
     *  · 降级规则：资源名，即限流规则的作用对象（对外提供接口APT），
     *            降级策略：可以选择 RT 、 异常比例 、 异常数
     *        ~ RT解释：平均响应时间（DEGRADE_GRADE_RT）: 当1s内持续进入5个请求，对应时刻的平均响应时间（秒级）均超过阈值（count,以ms为单位），
     *                       那么在接下的时间窗口（DegradeRule中的timeWindow，以s为单位）之内，对这个方法都会自动熔断（抛出DegradeException）。
     *                       注意Sentinel默认统计的RT上限是4900ms,超出此阈值都会算作4900ms,若需要变更此上限可以通过启动配置项
     *                       -Dcsp.sentinel.statistic.max.rt=xxx 来配置。
     *          例子： RT：填写200 ，时间窗口：填写 4
     *              作用：如果QPS大于5，且平均响应时间大于200ms,则接下来4s钟无法访问，之后恢复
     *        ~ 异常比例（DEGRADE_GRADE_EXCEPTION_PATIO）：当资源的每秒请求量大于等于5，并且每秒异常总数占通过量的比值超过阈值（DegradeRule
     *                  中的count）之后，资源进入降级状态，即在接下的时间窗口（DegradeRule中的timeWindow，以s为单位）之内，对这个方法的调用都会
     *                  自动地返回。异常比例的阈值[0.0, 1.0] , 代表 0% - 100% 。
     *        ~ 异常数（DEGRADE_GRADE_EXCEPTION_COUNT）：当资源近1分钟内的异常数目超过阈值之后会进行熔断。注意由于统计时间窗口是分钟级别的，若
     *                  timeWindow小于60s，则结束熔断状态后仍可能再进入熔断状态。
     *
     *  · 热点规则：资源名 hot ； 参数索引从0开始，对应 a 、 b ... ;
     *              设置 单机阈值 和 统计窗口时长
     *              参数例外项：可以对索引某些请求值设置特定的阈值
     *              详见 TestSentinelController : testHot() 方法
     *      热点规则适用于存在热点参数，也就是某些参数的QPS非常高，并且希望提升API可用性的场景。
     *      配置热点规则参数必须是基本类型或者String
     *          - 源码 ： com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowChecker#passCheck
     *
     * · 系统规则：
     *      ~ Load:当系统load1(1分钟的load)超过阈值，且并发线程数超过系统容量时触发，建议设置为CPU核心数 * 2.5 。（仅对Linux/Unix-like机器生效）
     *          系统容量 = maxQps * minRt ； maxQps : 秒级统计出来的最大QPS。 minRt : 秒级统计出来的最小响应时间。
     *      ~ RT:所有入口流量的平局RT达到阈值触发。
     *      ~ 线程数：所有入口流量的并发线程数达到阈值触发。
     *      ~ 入口QPS: 所有入口流量的QPS达到阈值触发。
     *
     * ---
     *
     * Sentinel 通信机制：查看Sentinel Dashboard.jpg
     *  · 在Sentinel控制台左侧菜单【机器列表】中，可以查看注册上来的机器清单：http://IP地址:端口号/api，可以查看到Sentinel客户端微服务提供的各种API
     *      注册/心跳发送：com.alibaba.csp.sentinel.transport.heartbeat.SimpleHttpHeartbeatSender
     *  · 通信API：com.alibaba.csp.sentinel.command.Command.Handler的实现类
     *
     * Sentinel通信特点：
     *  · 1).sentinel-transport-simple-http会在客户端主机上新端口8719
     *  · 2).当端口占用时端口逐次+1，直到可用
     *  · 3).第一次启动sentinel-tansport-simple-http会向Dashboard注册
     *  · 4).STSH默认间隔10秒钟向Dashboard发送心跳包通知健康状态
     *  · 5).http://客户端IP:端口号/api 对Dashboard暴露API接口供其调用
     *  · 6).Dashboard与STSH间采用REST风格通信
     *  · 7).因为涉及开辟新端口，所以不要忘记在客户端放行8719/8720等端口
     *
     * SentinelAPI 和 @SentinelResource 请详看 TestSentinelController 中的方法及文字描述，主要是测试应用
     *      文档地址：https://www.imooc.com/article/289384
     *
     * RestTemplate整合Sentinel
     *  · 需要在初始化 RestTemplate 时候增加 @SentinelRestTemplate 注解
     *      开关：resttemplate.sentinel.enabled=false : 关闭 @SentinelRestTemplate 注解
     *      @SentinelRestTemplate 提供了 blockHandler() 和 fallback() 实现熔断和降级
     *  · 实际测试方法请查看 TestSentinelController#testRestTemplateSetinel
     *  - 相关源码：com.alibaba.cloud.sentinel.custom.SentinelBeanPostProcessor
     *
     * Feign整合Sentinel
     *  · application.yml 中配置：feign.sentinel.enabled = true
     *      测试流程：访问 /shares/1 后查看 sentinel 控制台页面发现可以查看到 GET:http://user-center/users/{id} ，整合成功
     *      设置流控规则 后测试实现限流 ： Whitelabel Error Page
     *  · 限流降级放生时，定制自己的处理逻辑
     *      ~ 可以在 @FeignClient 注解中增加 fallback = XXX.class 或 fallbackFactory = XXX.class
     *          两者选择一个实现即可，fallbackFactory 可以打印异常 Throwable，推荐使用
     *      ~ 本次修改 UserCenterFeignClient 的 @FeignClient 分别测试增加 fallback 和 fallbackFactory
     *  - 相关源码：com.alibaba.cloud.sentinel.feign.SentinelFeign
     *
     * ---
     *
     * Sentinel 规则持久化：就是使配置的 Sentinel 限流降级规则每次系统重启的时候还存在，不丢失，适合生产。
     * · 拉模式：文档地址：https://www.imooc.com/article/289402 。
     *    ~ 原理：FileRefreshableDataSource 定时从指定文件中读取规则JSON文件图中的本地文件，如果发现文件发生变化，就更新规则缓存。
     *           FileWritableDataSource 接收控制台规则推送，并根据配置，修改规则JSON文件图中的本地文件。
     *    ~ 实现步骤：
     *      01). pom.xml 中引入相关依赖：sentinel-datasource-extension
     *      02). 编写拉模式规则持久化代码：FileDataSourceInit
     *      03). 配置：在项目的 resources/META-INF/services 目录下创建文件，
     *          文件名为 com.alibaba.csp.sentinel.init.InitFunc ，
     *          内容为：上面 FileDataSourceInit 的包名类名全路径（com.itmuch.contentcenter.sentineltest.FileDataSourceInit）即可。
     *    ~ 优缺点分析
     *      优点：简单易懂 ；没有多余依赖（比如配置中心、缓存等）
     *      缺点：由于规则是用 FileRefreshableDataSource 定时更新的，所以规则更新会有延迟。
     *           如果FileRefreshableDataSource定时时间过大，可能长时间延迟；如果FileRefreshableDataSource过小，又会影响性能；
     *           规则存储在本地文件，如果有一天需要迁移微服务，那么需要把规则文件一起迁移，否则规则会丢失。
     *    ~ 【不生效】：理论上应该是重启服务和Sentinel配置的规则都存在，但是不生效，需要到服务器进行一个测试。
     *       Sentinel 报错： HTTP request failed: http://192.168.88.1:8720/getRules?type=flow
     *       实际成产的时候不使用这种方式，后续再查一下这个问题...
     * · 推模式：文档地址：https://www.imooc.com/article/289464 。
     *   ~ 原理简述：
     *      控制台推送规则：将规则推送到 Nacos 或 其他远程配置中心
     *      Sentinel客户端链接Nacos，获取规则配置；并监听Nacos配置变化，如发生变化，就更新本地缓存（从而让本地缓存总是和Nacos一致）
     *      控制台监听Nacos配置变化，如发生变化就更新本地缓存（从而让控制台本地缓存总是和Nacos一致）
     *   ~ 实现步骤：需要同时修改微服务和Sentinel的代码，后续在做测试实现
     * · 生产环境使用Sentinel TODO
     *   ~ 01).建议使用 【推模式】
     *   ~ 02).使用阿里云提供的AHAS
     *      开通地址：https://ahas.console.aliyun.com/
     *      开通说明：https://help.aliyun.com/document_detail/90323.html
     */

    /**
     * Spring 实现异步的方法
     * · AsyncRestTemplate : 参考文档：https://blog.csdn.net/jiangchao858/article/details/86709750
     * · @Async 注解 : 参考文档：https://spring.io/guides/gs/async-method/
     * · WebClient (Spring 5.0引入) :
     *      参考文档：https://docs.spring.io/spring/docs/5.1.8.RELEASE/spring-framework-reference/web-reactive.html#webflux-client
     * · MQ : kafka、 RabbitMQ、 RocketMQ、 ActiveMQ ...
     *   ~ MQ适用场景：异步处理、 流量削峰填谷（秒杀场景）、 解耦微服务
     *   ~ MQ如何选择：https://www.imooc.com/article/290040
     *   ~ RocketMQ 4.5.1 安装文档：https://www.imooc.com/article/290089
     *   ~ RocketMQ控制台 4.5.1 安装文档：https://www.imooc.com/article/290092
     *   ~ RocketMQ控制台 使用文档：https://github.com/eacdy/rocketmq-externals/blob/master/rocketmq-console/doc/1_0_0/UserGuide_CN.md
     *   ~ RocketMQ博客地址：http://www.itmuch.com/books/rocketmq/operation.html
     * - 本次使用MQ实现异步处理，是为了学习Spring Cloud Alibaba的组件，实际开发中根据场景区分选择。
     *  RocketMQ-Console 前台：
     *      rocketmq.config.namesrvAddr=namesrv服务地址（ip1:port;ip2:port）
     *      java -jar rocketmq-console-ng-1.0.0.jar 启动 —当终端断了该服务就会停止
     *      nohup java -jar rocketmq-console-ng-1.0.0.jar >>/usr/logs/log.out 2>&1 & 后台启动 --当终端断了也不会停止服务
     *      nohup java -jar rocketmq-console-ng-1.0.0.jar &
     *      控制台访问路径：http://39.102.66.189:17890
     *
     * ---
     *
     * RocketMQ 开发者指南
     * https://github.com/apache/rocketmq/tree/master/docs/cn
     *
     * RocketMQ 术语/概念
     * · (Topic) 主题：一类消息的集合，RocketMQ的基本订阅单位
     * · 消息模型：~ Producer (生产者，生产消息)
     *           ~ Broker (消息代理，存储消息、转发消息)
     *           ~ Consumer (消费者，消费消息)
     * · 部署结构：~ Name Server (名字服务) : 生产者/消费者通过名字服务查找各主题响应的 Broker IP 列表
     *           ~ Broker Server (代理服务器) : 消息中转角色，负责存储消息、传发消息
     * · 消费模式：~ Pull Consumer (拉取式消费) : 应用调用 Consumer 的拉取信息方法从 Broker Server 拉取消息
     *           ~ Push Consumer (推动式消费) : Broker 收到消息后主动推送给消费端，该模式实时性较高
     * · (Group) 组：~ Producer Group (生产者组) : 同一类 Producer 的集合，这类 Producer 发送同一类消息且发送逻辑一致
     *              ~ Consumer Group (消费者组) : 同一类 Consumer 的集合，这类 Consumer 通常消费同一类消息
     * · 消息传播模式：~ Clustering (集群) : 相同 Consumer Group 的每个实例 Consumer 实例平均分摊消息
     *              ~ Broadcasting (广播) : 相同 Consumer Group 的每个 Consumer 实例都接收全量的消息
     * · 消息类型：普通消息、 顺序消息、 定时/延时消息、 事务消息
     *
     * ---
     *
     * 相关MQ对应使用操作文档：
     *   https://docs.spring.io/spring-boot/docs/2.1.6.RELEASE/reference/html/boot-features-messaging.html#boot-features-jms
     *
     * 编写生产者：
     *  · 01).pom.xml 中引入依赖： rocketmq-spring-boot-starter
     *  · 02).Application启动类上写注解：不需要
     *  · 03).application.yml 中实现配置：rocketmq.name-server
     *  · 04).业务实现逻辑：shareAdminService.auditById() -> convertAndSend("topic(主题)", "消息体")
     * 编写消费者：
     *  在 user-center 模块实现 ; 方法实现类：AddBonusListener
     *  · 01).pom.xml 中引入依赖： rocketmq-spring-boot-starter
     *  · 02).Application启动类上写注解：不需要
     *  · 03).application.yml 中实现配置：rocketmq.name-server
     *  · 04).方法实现逻辑：AddBonusListener 实现 RocketMQListener 接口，泛型传入 消息体
     *      @Service
     *      @RocketMQMessageListener(consumerGroup = "consumer-group", topic = "add-bonus")
     *      public class XXX_Listener implements RocketMQListener<?> {
     *          @Override
     *          public void onMessage(UserAddBonusMsgDTO message) {
     *              // 当收到消息的时候，执行的业务
     *          }
     *      }
     *
     * 分布式事务问题:
     *  · 原理查看：rocketmq-transactional.jpg
     *    只有A服务本地事务执行成功 ，B服务才能消费该message
     *  · 概念术语：
     *      ~ 半消息（Half(Prepare) Message）：暂时无法消费的消息。生产者将消息发送到了MQ server,
     *              但这个消息会被标记为“暂不能投递”状态，先存储起来；消费者不会去消费这条消息。（是一种特殊的消息）
     *      ~ 消息回查（Message Status Check）：网络断开或生产者重启可能导致丢失事务消息的第二次确认。
     *              当 MQ server 发现消息长时间处于半消息状态时，将向消息生产者发送请求，询问该消息的最终状态（提交或回滚）。
     *  · 消息三态：
     *      ~ Commit : 提交事务消息，消费者可以消费此消息
     *      ~ Rollback : 回滚事务消息，broker会删除该消息，消费者不能消费
     *      ~ UNKONWN : broker需要回查确认该消息的状态
     *  · 实现：生产者按如下修改；消费者不需要修改
     *      shareAdminService.auditByIdTrans() 和 AddBonusTransactionListener
     *      ~ rocketMQTemplate.sendMessageInTransaction : 实现分布式事务的方法发送消息
     *      ~ XXX_TransactionListener implements RocketMQLocalTransactionListener
     *          实现 executeLocalTransaction() : 用来执行本地事务的接口
     *            和 checkLocalTransaction() : 用来检查本地事务的接口
     *          使用注解 @RocketMQTransactionListener : txProducerGroup 一定要与 sendMessageInTransaction() 方法的值保持一致
     *  · 测试：
     *      debug 模式启动 content-center
     *      在 executeLocalTransaction() 返回 COMMIT 的代码行打上断点，进到该断点强制停止程序线程
     *      Terminal 强制停止方法：
     *          # jps
     *          # taskkill /PID [jps查出的线程ID] /F
     *
     * ---
     *
     * Spring Cloud Stream
     *  查看Spring Cloud Stream.jpg
     *  · 概念：一个用于构建消息驱动的微服务的框架。
     *  · 支持：kafka 、 RabbitMQ 、 RocketMQ
     * Spring Cloud Stream 编程模型：概念
     *  查看Spring Cloud Stream 1.jpg
     *  · Destination Binder (目标绑定器) : 与消息中间件通信的组件
     *  · Destination Bindings (目标绑定) : Binding 是连接应用程序跟消息中间件的桥梁，用于消息的消费和生产，由 binder 创建。
     *  · Message (消息)
     *  · Input 表示微服务接收消息；Output 表示微服务发送消息。
     *
     * Spring Cloud Stream 编写【生产者】：
     *  · 01).pom.xml 中引入依赖：spring-cloud-starter-stream-rocketmq
     *  · 02).Application启动类上写注解：@EnableBinding(Source.class)
     *  · 03).application.yml 中实现配置：spring.cloud.stream.rocketmq.binder 和 spring.cloud.stream.bindings.output
     *      注意配置文件中的 output 是因为与 接口 Source.class 中的 @Output("output") 的值一致。
     *  · 04).编写测试实现代码：TestStreamController#testStream()
     *      source.output().send(Message<?> messag) 为发送消息的实现；
     *      在 RocketMQ 控制台上 Message 中可以查看消费发送成功。
     * Spring Cloud Stream 编写【消费者】：
     *  在 user-center 模块实现 ;
     *  · 01).pom.xml 中引入依赖：spring-cloud-starter-stream-rocketmq
     *  · 02).Application启动类上写注解：@EnableBinding(Sink.class)
     *  · 03).application.yml 中实现配置：spring.cloud.stream.rocketmq.binder 和 spring.cloud.stream.bindings.input
     *      ~ 特别注意：消费者的topic设定destination的值一定要与生产的topic值一致。否则无法消费。
     *      ~ 说明：group 设置声明：如果用的是RocketMQ，一定要设置 ； 如果是其他的MQ，可以为空。
     *  · 04).编写测试实现代码：TestStreamConsumer#receive()
     *      @StreamListener(Sink.INPUT) : 通过注解 @StreamListener 实现多监听方法调度。
     *
     * 特殊说明：通过自定义学习，要理解上面的 output 和 input。
     *
     * Spring Cloud Stream 接口自定义 编写【生产者】：
     *  · 01).在上面 Spring Cloud Stream 编写【生产者】 基础上继续进行实现
     *  · 02).定义接口 MySource ：
     *      消息发送通道定义，定义了一个 MessageChannel 类型的 output() 方法，
     *      用 @Output 注解标示，并指定了 binding 的名称为 my-output 。
     *  · 03).Application启动类上写注解(增加一个MySource)：@EnableBinding({Source.class, MySource.class})
     *  · 04).application.yml 中增加配置：spring.cloud.stream.bindings.my-output
     *      ~ 注意：配置文件中的 my-output 一定要与 接口 MySource 中注解 @Output(MY_OUTPUT) 中引用的值一致。
     *  · 05).编写测试实现代码：TestStreamController#testStreamMySource()
     *  · ~ 产生错误：运行调用报错：Invalid bound statement (not found): com.itmuch.contentcenter.rocketmq.MySource.output
     *      报错是MyBatis的异常：错误原因是 @MapperScan("com.itmuch") 扫描的原因，按照 步骤 06) 进行修改即可。
     *  · 06).修改 @MapperScan 扫描包为，缩小扫描范围：@MapperScan("com.itmuch.contentcenter.dao")
     *      在 RocketMQ 控制台上 Message 中可以查看消费发送成功。
     * Spring Cloud Stream 接口自定义 编写【消费者】：
     *  在 user-center 模块实现 ;
     *  · 01).在上面 Spring Cloud Stream 编写【消费者】 基础上继续进行实现
     *  · 02).定义接口 MySink ：
     *  · 03).Application启动类上写注解(增加一个MySink)：@EnableBinding({Sink.class, MySink.class})
     *  · 04).application.yml 中增加配置：spring.cloud.stream.bindings.my-input
     *      ~ 特别注意：消费者的topic设定destination的值一定要与生产的topic值一致。否则无法消费。
     *      ~ 注意：配置文件中的 my-input 一定要与 接口 MySource 中注解 @Input(MY_INPUT) 中引用的值一致。
     *      ~ 说明：group 设置声明：如果用的是RocketMQ，一定要设置 ； 如果是其他的MQ，可以为空。
     *  · 05).编写测试实现代码：TestStreamMySourceConsumer#receive()
     *      @StreamListener(MySink.MY_INPUT) : 通过注解 @StreamListener 实现多监听方法调度。
     *
     * Spring Cloud Stream 本质：
     *  · Source 接口 ：是 Spring Cloud Stream 默认提供的一个消息发送的接口。 自定义的 MySource 接口没什么区别。
     *  · Sink 接口 ：是 Spring Cloud Stream 默认提供的一个消息接收的接口。 自定义的 MySink 接口没什么区别。
     *  · Processor 接口 ：继承了 Source 、Sink 接口。可以使用消息发送 、接收。
     *  · 本质：当我们定义好 Source/Sink 接口后，在启动类使用 EnableBinding 指定了接口后，
     *          就会使用 IOC 创建对应名字的代理类，所以配置文件中也必须同名。
     *
     * ---
     *
     * Spring Cloud Stream 消息过滤：
     * - 手记：https://www.imooc.com/article/290424
     *   实现消息消费的过滤例子：实现消息的分流处理：生产者生产的消息，虽然消息体可能一样，但是header不一样。
     *   可编写两个或者更多的消费者，对不同header的消息做针对性的处理！
     *   生产者：content-center ; 消费者：user-center
     * · 方式(1)：condition 方式：都使用 Sink 进行测试：
     *  ~ 生产者：设置一下header，比如my-header，值根据你的需要填写：my-condition-header
     *  ~ 消费者：@StreamListener(value = Sink.INPUT, condition = "headers['my-header']=='my-condition-header'")
     * 	        使用 StreamListener 注解的 condition 属性。当 headers['my-header']=='my-condition-header' 条件满足，才会进入到方法体。
     * · 方式(2)：Tags 方式: 该方式只支持RoketMQ，不支持Kafka/RabbitMQ
     *  ~ 生产者：设置一下header : setHeader(RocketMQHeaders.TAGS, "tag1") : 需要注意：只能设置1个tag
     *          防止代码混乱，新创建一个接口：MyTagsSource
     *  ~ 消费者：
     *      ~ 01).创建自定义消息消费接口：MyTagsSink : 设定两个 Input
     *      ~ 02).application.yml 中增加配置：
     *          spring.cloud.stream.rocketmq.bindings.my_tags_input1 和 my_tags_input2
     *          spring.cloud.stream.bindings.my_tags_input1 和 my_tags_input2
     *      ~ 03).创建实现类：TestStreamTagsConsumer
     * · 方式(3)：Sql 92 : 该方式只支持RoketMQ，不支持Kafka/RabbitMQ。注意用了sql，就不要用Tag。
     *      官方文档：http://rocketmq.apache.org/rocketmq/filter-messages-by-sql92-in-rocketmq/
     *      具体请查看手记及官方文档学习使用。
     *
     * Spring Cloud Stream 监控：
     *  http://localhost:8010/actuator
     *      http://localhost:8010/actuator/bindings
     *      http://localhost:8010/actuator/channels ： 其实就是 output
     *  http://localhost:8010/actuator/health
     *
     * Spring Cloud Stream 异常处理：
     * · 手记：https://www.imooc.com/article/290435
     * TODO 后续可以尝试进行RetryTemplate重试配置实现  <input channel名称>: 分析应该是在消息生产者实现配置 使用的消费者的group名称？？？
     * 全局
     *
     * ---
     *
     * Spring Cloud Stream + RocketMQ 实现分布式事务
     * · Stream 没有提供分布式事务的能力，分布式事务是 RocketMQ 提供的能力。
     * · content-center 和 user-center ：application.yml 中 spring 消息模型整合 rocketmq 的配置 可以不需要了。暂时注释掉。
     * · 使用本身提供的 Source 即可以实现，但是为了保留测试代码，新建一个自定义的 Source : MyMqSource -> topic 为 new-add-bonus
     * · 使用本身提供的 Sink 即可以实现，但是为了保留测试代码，新建一个自定义的 Source : MyMqSink -> topic 为 new-add-bonus
     * · content-center 和 user-center ：中的 destination 保持一致
     *
     * 【重构】消息生产者
     * · 01).实际就是使用 Stream 替换 rocketMQTemplate.sendMessageInTransaction 逻辑即可
     *      shareAdminService#auditByIdStreamMqTrans() :
     *          myMqSource.output().send(Message<?> message) 无法参入 arg 值，将值放在 header 中处理
     * · 02).application.yml 中配置
     *      ~ spring.cloud.stream.bindings.my-mq-output.destination=new-add-bonus
     *      ~ spring.cloud.stream.rocketmq.bindings.my-mq-output.producer
     *          使用 transcational: true 实现事务 ；group 标明组，与txProducerGroup值保持一致。
     * · 03).新编写 NewAddBonusTransactionListener 实现类：
     *      使用注解 @RocketMQTransactionListener : txProducerGroup 一定要与 spring.cloud.stream.rocketmq.bindings.my-mq-output.producer.group 配置的值保持一致
     * · 【特别注意】：需要注释掉 rocketmq.name-server 和 rocketmq.producer ； 同时在 ShareAdminService 中暂时注释掉 RocketMQTemplate
     *      否则会报错：client.exception.MQClientException: The producer group has been created before, specify another name please.
     *
     * 【重构】消息消费者
     * 在 user-center 模块实现 ;
     * · 01).application.yml 中配置
     *      ~ spring.cloud.stream.bindings.my-mq-input.destination=new-add-bonus
     * · 02).新编写 NewAddBonusListener 实现类：
     *      ~ @StreamListener(MyMqSink.MY_MQ_INPUT)
     *        public void receive(UserAddBonusMsgDTO message) {
     *          // 重构本地事务逻辑
     *        }
     *
     * ---
     *
     * Spring Cloud Stream 知识盘点：
     * · 手记：https://www.imooc.com/article/290489
     *
     */

    /**
     * Spring Cloud Gateway :
     * · 是Spring Cloud的网关（第二代），未来会取代Zuul（第一代）
     * · 基于 Netty 、 Reactor 以及 WebFlux 构建
     *     ~ Netty ：网络通信框架，可以实现高性能的服务端和客户端
     *     ~ Reactor ：是一个 Reactive 编程模型的实现，正在越来越流行
     *     ~ WebFlux ：是一个 Reactive 的 Web 框架
     * · 优点：
     *     ~ 性能强劲 ：是第一代网关Zuul 1.x的1.6倍！性能PK : https://www.imooc.com/article/285068
     *     ~ 功能强大 ：内置了很多实用功能，比如转发、监控、限流等
     *     ~ 设置优雅，易扩展
     * · 缺点：
     *     ~ 依赖 Netty 与 WebFlux , 不是 Servlet 编程模型，有一定的适应成本
     *     ~ 不能在 Servlet 容器下工作，也不能构建成 WAR 包
     *     ~ 不支持 Spring Boot 1.x
     * · 转发规则：
     *     访问 ${GATEWAY_URL}/{微服务X}/** 会转发到 微服务X的/**路径
     *
     * 创建网关 gateway 工程。
     *      后续 Spring Cloud Gateway 相关知识点请在 gateway 工程查看。
     */

    /**
     * 微服务的用户认证与授权
     * 主要实现在 user-center 模块，可以进行查阅
     *
     * JWT操作工具类：
     *  ~ 手记：https://www.imooc.com/article/290892
     * 用户中心引入JWT实现逻辑
     *  · 01).pom.xml 中引入依赖：jjwt-api 、 jjwt-impl 、 jjwt-jackson
     *  · 02).创建工具包：JwtOperator
     *  · 03).application.yml中配置jwt：jwt.secret:秘钥 和 jwt.expire-time-in-second:有效期
     * 用户中心按照上述整合jwt , 注意秘钥[jwt.secret:秘钥]需要保持一致
     *
     * AOP实现登录检查
     *  · 01).引入依赖：spring-boot-starter-aop
     *  · 02).创建注解：CheckLogin
     *  · 03).创建切面：CheckLoginAspect (后续会修改成AuthAspect)
     * 统一管理异常：
     *  · 01).GlobalExceptionErrorHandler
     *
     * Feign 传递 Token
     *  · 方式1. @RequestHeader
     *      优点：修改简单。缺点：需要修改feign的定义，多个api的时候修改工作量大。
     *  · 方式2. 实现接口： RequestInterceptor
     *      优点：实现全局用意配置处理，不需要修改具体的业务代码。
     *      实现类：TokenRelayRequestInterceptor 实现传递 token
     *             配置Feign , 可以通过 @FeignClient(configuration = XXX.class) 或者在 application.xml 中实现配置
     *             本次选择在配置文件 application.xml 进行全局配置。
     *
     * RestTemplate 传递 Token
     *  · 方式1. exchange()
     *      优点：修改简单。缺点：多个api的时候修改工作量大。
     *      实现：TestSentinelController 中 tokenRelay()
     *  · 方式2. 实现接口： ClientHttpRequestInterceptor
     *      实现：创建拦截器 RestTemplateTokenRelayInterceptor ，
     *           在启动类创建RestTemplate时候设置interceptor ： restTemplate.setInterceptors()
     *
     * AOP实现用户权限验证 (在AOP实现登录检查基础上修改)
     *  · 01).创建注解：CheckAuthorization
     *  · 02).修改切面：CheckLoginAspect 修改成AuthAspect ，修改现有方法进行封装，增加权限验证逻辑
     *  · 03).方法中增加注解：ShareAdminController#auditById() -加注解-> @CheckAuthorization("admin")
     *
     */

    /**
     * 使用 Nacos 管理配置 ：实现配置
     *  · 01).引入依赖：spring-cloud-starter-alibaba-nacos-config
     *  · 02).写配置：spring cloud alibaba 采用约定大于配置的方式管理配置
     *        ~ 创建：bootstrap.yml ，实现相关配置，注意这里面是 config
     *        - 说明：bootstrap.yml 中的 server-addr 是用作于【配置管理】的：spring.cloud.nacos.config
     *               application.yml 中的 server-addr 是用作于【服务注册】的：spring.cloud.nacos.discovery
     *  · 03).注释掉 application.yml 中的 spring.application.name
     *  · 04).编写测试类：TestController#testConfiguration()
     *  · 05).查看 nacos-bootstrap-config-standard.jpg 进行配置
     *  · 06).如果报错：o.s.c.a.n.c.NacosPropertySourceBuilder : get data from Nacos error,dataId:content-center-dev.yaml
     *      参考解决：查看Linux对应的全局字符文件(vim /etc/locale.conf): 修改成：LANG=zh_CN.UTF-8 （已经修改）
     *              启动时候：java -Dfile.encoding=utf-8 -jar test.jar （未进行修改）
     *
     * 使用 Nacos 管理配置 ：配置属性动态刷新
     *  · 01).通过注解 @RefreshScope 实现：在需要修改的属性的类上增加 @RefreshScope 注解。
     *
     * 使用 Nacos 管理配置 ：配置共享 ：相同应用不同环境的共享
     *  · 启动日志:Located property source: [BootstrapPropertySource {name='bootstrapProperties-content-center-dev.yaml,DEFAULT_GROUP'},
     *                                      BootstrapPropertySource {name='bootstrapProperties-content-center.yaml,DEFAULT_GROUP'}]
     *  · 分析启动日志：content-center-dev.yaml 是 dev 环境下的指定配置 ； content-center.yaml 是通用配置。
     *  · 优先级：指定配置的优先级 > 通用配置的优先级
     * 使用 Nacos 管理配置 ：配置共享 ：应用间的共享
     *  · 方式1. shared-dataids : resources/config-share/bootstrap-shared-dataids.yml
     *  · 方式2. ext-config : resources/config-share/bootstrap-ext-config.yml
     *  · 选择一个实现方式，将内容复制粘贴到 bootstrap.yml 中。
     *  · 优先级：shared-dataids < ext-config < 自动
     *
     * 引导上下文：
     *  · 连接配置服务器，读取外部配置
     *  · Application Context 的父上下文
     *  · 远程配置 & 本地配置优先级
     *      - 查看 nacos-bootstrap-config-yml.jpg ，
     *      - 特殊注意：一定要放在远程配置（nacos的配置文件）中才会生效。
     *
     * Nacos数据持久化：
     *  · 服务发现组件 ：~/nacos/naming
     *  · 配置服务器 ：
     *      - 配置数据 ：$NACOS_HOME/data/derby-data
     *      - 快照等 ：~/nacos/config
     *      存储在derby的数据不能用于生产环境：derby是内嵌式数据库，不能高可用
     *
     * 搭建生产可用的Nacos集群
     *  · 手记：https://www.imooc.com/article/288153
     *
     * 最佳实践总结
     *  · 能放本地，不放远程
     *  · 尽量规避优先级
     *  · 定规范，例如所有的配置属性都要加上注释
     *  · 配置管理人员尽量少
     *
     */

}
