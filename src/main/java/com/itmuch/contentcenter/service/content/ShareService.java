package com.itmuch.contentcenter.service.content;

import com.itmuch.contentcenter.dao.content.ShareMapper;
import com.itmuch.contentcenter.domain.dto.content.ShareDTO;
import com.itmuch.contentcenter.domain.dto.user.UserDTO;
import com.itmuch.contentcenter.domain.entity.content.Share;
import com.itmuch.contentcenter.feignclient.UserCenterFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShareService {

    private final ShareMapper shareMapper;

    private final RestTemplate restTemplate;

    private final DiscoveryClient discoveryClient;

    private final UserCenterFeignClient userCenterFeignClient;

    private final RocketMQTemplate rocketMQTemplate;

    public ShareDTO findById(Integer id) {
        //return findByIdUrl(id);
        //return findByIdByInstance(id);
        //return findByIdByRibbon(id);
        return findByIdByFeign(id);
    }

    /**
     * 直接通过 ip 地址取出
     * @param id
     * @return
     */
    public ShareDTO findByIdUrl(Integer id) {
        // 获取分享详情
        Share share = shareMapper.selectByPrimaryKey(id);

        // 发布人id
        Integer userId = share.getUserId();
        UserDTO userDTO = restTemplate.getForObject(
                "http://localhost:8080/users/{id}",
                UserDTO.class,
                userId);

        ShareDTO shareDTO = new ShareDTO();
        //消息装配
        BeanUtils.copyProperties(share, shareDTO);
        shareDTO.setWxNickname(userDTO.getWxNickname());

        return shareDTO;
    }

    /**
     * 通过 discoveryClient 获取 user-center 的 instance
     * 通过 instance 获取地址进行查询
     * 实现简单负载算法
     * @param id
     * @return
     */
    public ShareDTO findByIdByInstance(Integer id) {
        // 获取分享详情
        Share share = shareMapper.selectByPrimaryKey(id);

        // 用户中心所有实例信息
        // 通过 discoveryClient 获取 user-center 的 instance
        List<ServiceInstance> instances = discoveryClient.getInstances("user-center");
        // lambda 表达式获取请求的目标地址 url, 取出第一个，换成取出全部，编写负载算法随机取出一个
        /*String targetUrl = instances.stream()
                .map(instance -> instance.getUri().toString() + "/users/{id}")
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("当前服务未被发现"));*/

        // lambda 表达式获取所有用户请求
        List<String> targetUrlS = instances.stream()
                .map(instance -> instance.getUri() + "/users/{id}")
                .collect(Collectors.toList());
        // 负载均衡算法：生成一个随机数取出一个 url 地址
        int i = ThreadLocalRandom.current().nextInt(targetUrlS.size());
        String targetUrl = targetUrlS.get(i);

        log.info("请求的目标地址：{}", targetUrl);

        // 发布人id
        Integer userId = share.getUserId();
        UserDTO userDTO = restTemplate.getForObject(
                targetUrl,
                UserDTO.class,
                userId);

        ShareDTO shareDTO = new ShareDTO();
        //消息装配
        BeanUtils.copyProperties(share, shareDTO);
        shareDTO.setWxNickname(userDTO.getWxNickname());

        return shareDTO;
    }

    /**
     * 通过 ribbon 获取信息
     * Ribbon : Netflix 开源的客户端负载均衡器
     * @param id
     * @return
     */
    public ShareDTO findByIdByRibbon(Integer id) {
        // 获取分享详情
        Share share = shareMapper.selectByPrimaryKey(id);

        // 发布人id
        Integer userId = share.getUserId();
        UserDTO userDTO = restTemplate.getForObject(
                "http://user-center/users/{id}",
                UserDTO.class,
                userId);

        ShareDTO shareDTO = new ShareDTO();
        // 消息装配
        BeanUtils.copyProperties(share, shareDTO);
        shareDTO.setWxNickname(userDTO.getWxNickname());
        return shareDTO;
    }

    /**
     * 通过 feign 方式调用用户中心
     * @param id
     * @return
     */
    public ShareDTO findByIdByFeign(Integer id) {
        // 获取分享详情
        Share share = shareMapper.selectByPrimaryKey(id);

        // 发布人id
        Integer userId = share.getUserId();
        UserDTO userDTO = userCenterFeignClient.findById(userId);

        ShareDTO shareDTO = new ShareDTO();
        //消息装配
        BeanUtils.copyProperties(share, shareDTO);
        shareDTO.setWxNickname(userDTO.getWxNickname());

        return shareDTO;
    }

    public static void main(String[] args) {
        RestTemplate restTemplate1 = new RestTemplate();
        UserDTO forObject = restTemplate1.getForObject(
                "http://localhost:8080/users/{id}",
                UserDTO.class,
                1);
        System.out.printf(forObject.toString());
    }

}
