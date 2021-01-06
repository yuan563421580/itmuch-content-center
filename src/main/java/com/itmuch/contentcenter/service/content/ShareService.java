package com.itmuch.contentcenter.service.content;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.itmuch.contentcenter.dao.content.MidUserShareMapper;
import com.itmuch.contentcenter.dao.content.ShareMapper;
import com.itmuch.contentcenter.domain.dto.content.ShareDTO;
import com.itmuch.contentcenter.domain.dto.user.UserAddBonseDTO;
import com.itmuch.contentcenter.domain.dto.user.UserDTO;
import com.itmuch.contentcenter.domain.entity.content.MidUserShare;
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

import javax.servlet.http.HttpServletRequest;
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

    private final MidUserShareMapper midUserShareMapper;


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

    /**
     * 分页查询
     * @param title
     * @param pageNo
     * @param pageSize
     * @return
     */
    public PageInfo<Share> q(String title, Integer pageNo, Integer pageSize, Integer userId) {

        // 要开始分页了...
        // 它会切入下面这条不分页的SQL，自动拼接分页的SQL
        // 本质是Mybatis的拦截器，自动加上了limit
        PageHelper.startPage(pageNo, pageSize);

        // 不分页的SQL
        List<Share> shares = this.shareMapper.selectByParam(title);


        List<Share> sharesDeal;
        // 1. 如果用户未登录，那么downloadUrl全部设为null
        if (userId == null) {
            sharesDeal = shares.stream()
                    .peek(share -> {
                        share.setDownloadUrl(null);
                    })
                    .collect(Collectors.toList());
        }
        // 2. 如果用户登录了，那么查询一下mid_user_share，如果没有数据，那么这条share的downloadUrl也设为null
        else {
            sharesDeal = shares.stream()
                    .peek(share -> {
                        MidUserShare midUserShare = this.midUserShareMapper.selectOne(
                                MidUserShare.builder()
                                        .userId(userId)
                                        .shareId(share.getId())
                                        .build()
                        );
                        if (midUserShare == null) {
                            share.setDownloadUrl(null);
                        }
                    })
                    .collect(Collectors.toList());
        }

        // PageInfo 是一个工具类
        PageInfo<Share> sharePageInfo = new PageInfo<>(sharesDeal);
        return sharePageInfo;
    }

    /**
     * 积分兑换指定ID
     * @param id
     * @param request
     * @return
     */
    public Share exchangeById(Integer id, HttpServletRequest request) {
        Object userId = request.getAttribute("id");
        Integer integerUserId = (Integer) userId;

        // 1. 根据id查询share，校验是否存在
        Share share = this.shareMapper.selectByPrimaryKey(id);
        if (share == null) {
            throw new IllegalArgumentException("该分享不存在！");
        }
        Integer price = share.getPrice();

        // 2. 如果当前用户已经兑换过该分享，则直接返回
        MidUserShare midUserShare = this.midUserShareMapper.selectOne(
                MidUserShare.builder()
                        .shareId(id)
                        .userId(integerUserId)
                        .build()
        );
        if (midUserShare != null) {
            return share;
        }

        // 3. 根据当前登录的用户id，查询积分是否够
        UserDTO userDTO = this.userCenterFeignClient.findById(integerUserId);
        if (price > userDTO.getBonus()) {
            throw new IllegalArgumentException("用户积分不够用！");
        }

        // 4. 扣减积分 & 往mid_user_share里插入一条数据
        this.userCenterFeignClient.addBonus(
                UserAddBonseDTO.builder()
                        .userId(integerUserId)
                        .bonus(0 - price)
                        .build()
        );
        this.midUserShareMapper.insert(
                MidUserShare.builder()
                        .userId(integerUserId)
                        .shareId(id)
                        .build()
        );
        return share;
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
