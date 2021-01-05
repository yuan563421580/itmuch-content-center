package com.itmuch.contentcenter.controller.test;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.itmuch.contentcenter.domain.dto.user.UserDTO;
import com.itmuch.contentcenter.sentineltest.TestSentinelControllerBlockHandlerClass;
import com.itmuch.contentcenter.sentineltest.TestSentinelControllerFallBackClass;
import com.itmuch.contentcenter.service.test.TestSentinelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 编写测试sentinel的用例
 */
@Slf4j
@RestController
public class TestSentinelController {

    public static void main(String[] args) {
        TestSentinelController.restForSentinel();
    }

    /**
     * 用于测试 限流规则为 关联
     */
    public static void restForSentinel() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            for (int i = 0; i < 1000; i++) {
                String forObject = restTemplate.getForObject("http://localhost:8010/actuator/sentinel", String.class);
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Autowired
    private TestSentinelService testSentinelService;

    /**
     * 用于测试关联规则
     * @return
     */
    @GetMapping("/test-a")
    public String testA() {
        testSentinelService.testCommon();
        return "test-a";
    }

    /**
     * 用于测试关联规则
     * @return
     */
    @GetMapping("/test-b")
    public String testB() {
        testSentinelService.testCommon();
        return "test-b";
    }

    /**
     * 用于测试热点规则
     *  资源名 hot ； 参数索引从0开始，对应 a 、 b ... ;
     *  单机阈值 和 统计窗口时长
     *  测试路径：http://localhost:8010/test-hot?a=5&b=2
     *
     *  参数例外项：可以对索引某些请求值设置特定的阈值
     *      当设定a=aaa，限流阈值为100，访问下面路径则当每秒达到100才限流
     *  测试路径：http://localhost:8010/test-hot?a=aaa&b=2
     *
     * @param a
     * @param b
     * @return
     */
    @GetMapping("/test-hot")
    @SentinelResource("hot")
    public String testHot(@RequestParam(required = false) String a,
                          @RequestParam(required = false) String b) {
        return a + " " + b;
    }

    /**
     * 使用代码添加授权规则
     * 给 /shares/1 增加一个规则
     * @return
     */
    @GetMapping("/test-add-flow-rule")
    public String testAddFlowRule() {
        this.initFlowQpsRule("/shares/1");
        return "success";
    }

    private void initFlowQpsRule(String resourceName) {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule(resourceName);
        // set limit qps to 20
        rule.setCount(20);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setLimitApp("default");
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }

    /**
     * 测试 sentinel api
     *
     * SphU.entry 定义了资源，Sentinel 会对资源进行监控
     * 默认情况下Sentinel只会统计BlockException或者BlockException的子类
     * Tracer.trace(Throwable e) 用于统计特定异常发生的次数、发生占比 ...
     * ContextUtil
     *
     * 需要配合 Sentinel 控制台一起使用，对应 api 选择降级规则 ：异常比例 0.1 ; 时间窗口 1
     *
     * 快速刷新 浏览器进行测试
     * 测试现象：展示内容：参数非法 -> 限流，或者降级了
     *
     * ---上面为测试第1版、下面补充测试第2版---
     *
     * 增加来源 : ContextUtil.enter(String name, String origin)
     *
     * 需要配合 Sentinel 控制台一起使用，对应 api 选择流控规则 ：
     *  测试配置01). 针对来源 test-wfw ; 单机阈值 1
     *  测试现象01). 展示内容：参数非法 -> 限流，或者降级了
     *
     *  测试配置02). 针对来源 other-test-wfw ; 单机阈值 1
     *  测试现象02). 展示内容：参数非法
     *
     * 实际测试没有生效，不清楚原因，官方和论坛都没有查到，后续回来处理这个问题
     *
     * @param a
     * @return
     */
    @GetMapping("/test-sentinel-api")
    public String testSentinelAPI(@RequestParam(required = false) String a) {

        // 资源名称
        String resourceName = "test-sentinel-api";

        // 增加来源
        ContextUtil.enter(resourceName, "test-wfw");

        // 定义一个sentinel保护的资源，名称是test-sentinel-api
        Entry entry = null;
        try {
            entry = SphU.entry(resourceName);

            if (StringUtils.isBlank(a)) {
                throw new IllegalArgumentException("a is blank");
            }
            return a;
        } catch (BlockException e) {
            // 如果被保护的资源被限流或者降级了，就会抛BlockException
            log.warn("限流，或者降级了", e);
            return "限流，或者降级了";
        } catch (IllegalArgumentException e2) {
            // 统计 IllegalArgumentException 发生的次数、发生占比 ...
            Tracer.trace(e2);
            return "参数非法";
        } finally {
            if (entry != null) {
                // 退出 entry
                entry.exit();
            }

            ContextUtil.exit();
        }
    }

    /**
     * 使用 @SentinelResource 注解 实现 testSentinelAPI 相同的功能
     *      用于优化 testSentinelAPI 逻辑
     *
     * @SentinelResource 不支持 来源
     *
     * @SentinelResource 注解会默认 trace 所有的 Throwable ：Tracer.trace(Throwable e);
     *
     * blockHandler  处理限流或降级 ； 要求方法 相同的参数类型 和 相同的返回值类型。
     * fallback 处理降级 ； 要求方法 相同的参数类型 和 相同的返回值类型。
     *
     * 需要在 Sentinel 控制台分别配置流控规则和降级规则 配合测试
     *
     * ---继续进行优化---
     *
     * 优化内容为了只关注业务，优化本类中非必要代码
     *
     * 使用 blockHandlerClass
     *
     * 阅读：https://www.imooc.com/article/289384
     * @param a
     * @return
     */
    @GetMapping("/test-sentinel-resource")
    @SentinelResource(value = "test-sentinel-api",
            blockHandler = "block",
            blockHandlerClass = TestSentinelControllerBlockHandlerClass.class,
            fallback = "fallback",
            fallbackClass = TestSentinelControllerFallBackClass.class)
    public String testSentinelRs(@RequestParam(required = false) String a) {
        if (StringUtils.isBlank(a)) {
            throw new IllegalArgumentException("a cannot be blank");
        }
        return a;
    }


    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/test-rest-template-setinel/{userId}")
    public UserDTO testRestTemplateSetinel(@PathVariable Integer userId) {
        UserDTO userDTO = restTemplate.getForObject(
                "http://user-center/users/{userId}",
                UserDTO.class,
                userId);
        return userDTO;
    }

    @GetMapping("/tokenRelay/{userId}")
    public ResponseEntity<UserDTO> tokenRelay(@PathVariable Integer userId,
                                              HttpServletRequest request) {

        /*RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes attributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = attributes.getRequest();*/

        // 获取token
        String token = request.getHeader("X-Token");

        // 创建header
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Token", token);

        return restTemplate.exchange(
                "http://user-center/users/{userId}",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserDTO.class,
                userId
        );
    }

}
