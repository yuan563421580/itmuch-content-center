package com.itmuch.contentcenter.controller.test;

import com.itmuch.contentcenter.dao.content.ShareMapper;
import com.itmuch.contentcenter.domain.dto.user.UserDTO;
import com.itmuch.contentcenter.domain.entity.content.Share;
import com.itmuch.contentcenter.feignclient.TestBaiduFeignClient;
import com.itmuch.contentcenter.feignclient.TestUserCenterFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

// @RequiredArgsConstructor(onConstructor = @__(@Autowired)) 相当于 @Autowired(requird=false)
@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RefreshScope
public class TestController {

    //@Autowired(required = false)
    private final ShareMapper shareMapper;

    private final DiscoveryClient discoveryClient;

    private final TestUserCenterFeignClient testUserCenterFeignClient;

    private final TestBaiduFeignClient testBaiduFeignClient;

    @GetMapping("/test")
    public List<Share> testInsert() {
        // 1. 做插入
        Share share = new Share();
        share.setCreateTime(new Date());
        share.setUpdateTime(new Date());
        share.setTitle("xxx");
        share.setCover("xxx");
        share.setAuthor("大目");
        share.setBuyCount(1);

        shareMapper.insertSelective(share);

        // 2. 做查询: 查询当前数据库所有的share  select * from share ;
        List<Share> shares = shareMapper.selectAll();

        return shares;
    }

    /**
     * 测试：服务发现，证明内容中心总是可以找到用户中心
     * @return 用户中心所有实例地址信息
     */
    @GetMapping("/test2")
    public List<ServiceInstance> getInstances() {
        // 查询指定服务的所有实例信息
        // consul/eureka/zookeeper ...
        List<ServiceInstance> instances = discoveryClient.getInstances("user-center");
        return instances;
    }

    /**
     * 测试：查询当前服务发现组件注册了哪些微服务
     * @return
     */
    @GetMapping("/getService")
    public List<String> getService() {
        List<String> services = discoveryClient.getServices();
        return services;
    }

    // /test-get?id=1&wxId=aaa
    @GetMapping("/test-get")
    public UserDTO query(UserDTO userDTO) {
        return testUserCenterFeignClient.query(userDTO);
    }

    @GetMapping("/test-get2")
    public UserDTO query2(@RequestParam("id") Integer id, @RequestParam("wxId") String wxId) {
        return testUserCenterFeignClient.query(id, wxId);
    }

    @GetMapping("/test-get-post")
    public UserDTO testGetPost(@RequestParam("id") Integer id, @RequestParam("wxId") String wxId) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(id);
        userDTO.setWxId(wxId);
        return testUserCenterFeignClient.post(userDTO);
    }

    @GetMapping("/baiduIndex")
    public String baiduIndex() {
        return testBaiduFeignClient.index();
    }

    @Value("${your.configuration}")
    private String yourConfiguration;

    @GetMapping("/test-config")
    public String testConfiguration() {
        return this.yourConfiguration;
    }

}
