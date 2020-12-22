package com.itmuch.contentcenter.service.content;

import com.itmuch.contentcenter.dao.content.ShareMapper;
import com.itmuch.contentcenter.dao.messaging.RocketmqTransactionLogMapper;
import com.itmuch.contentcenter.domain.dto.content.ShareAuditDTO;
import com.itmuch.contentcenter.domain.dto.content.ShareDTO;
import com.itmuch.contentcenter.domain.dto.messaging.UserAddBonusMsgDTO;
import com.itmuch.contentcenter.domain.dto.user.UserDTO;
import com.itmuch.contentcenter.domain.entity.content.Share;
import com.itmuch.contentcenter.domain.entity.messaging.RocketmqTransactionLog;
import com.itmuch.contentcenter.domain.enums.AuditStatusEnum;
import com.itmuch.contentcenter.feignclient.UserCenterFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

    private final RocketmqTransactionLogMapper rocketmqTransactionLogMapper;

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

    //transactional
    @Transactional(rollbackFor = Exception.class)
    public Share auditById(Integer id, ShareAuditDTO auditDTO) {

        // 保留下面的处理逻辑，可以和分布式事务的相关逻辑进行对比学习

        // 1. 查询share是否存在，不存在或者当前的audit_status != NOT_YET，那么抛异常
        Share share = this.shareMapper.selectByPrimaryKey(id);
        if (share == null) {
            throw new IllegalArgumentException("参数非法！该分享不存在！");
        }
        if (!Objects.equals("NOT_YET", share.getAuditStatus())) {
            throw new IllegalArgumentException("参数非法！该分享已审核通过或审核不通过！");
        }

        // 2.审核资源，将状态设为PASS/REJECT
        share.setAuditStatus(auditDTO.getAuditStatusEnum().toString());
        this.shareMapper.updateByPrimaryKey(share);

        // 3. (作废) 如果是PASS，那么为发布人添加积分
        //userCenterFeignClient.addBonus(id, 500);

        // 分析功能：主要功能是审核，加积分可以改成异步操作
        // 本次使用MQ 是为了学习Spring Cloud Alibaba的组件，实际开发中根据场景区分选择
        // [内容中心] ---(生产消息)---> [MQ] ---(消费消息)---> [用户中心（监听）]

        // 3. (RocketMQ) 如果是PASS，那么发送消息给rocketmq，让用户中心去消费，并为发布人添加积分
        // 参数 : convertAndSend("topic(主题)", "消息体")
        rocketMQTemplate.convertAndSend("add-bonus",
                UserAddBonusMsgDTO.builder()
                    .userId(share.getUserId())
                    .bonus(50)
                .build()
        );


        return share;
    }

    // 分布式事务实现逻辑 ：RocketMQ 发送半消息
    // 不需要 @Transactional ，因为依赖了 RocketMQ 的分布式事务
    public Share auditByIdTrans(Integer id, ShareAuditDTO auditDTO) {
        // 1. 查询share是否存在，不存在或者当前的audit_status != NOT_YET，那么抛异常
        Share share = this.shareMapper.selectByPrimaryKey(id);
        if (share == null) {
            throw new IllegalArgumentException("参数非法！该分享不存在！");
        }
        if (!Objects.equals("NOT_YET", share.getAuditStatus())) {
            throw new IllegalArgumentException("参数非法！该分享已审核通过或审核不通过！");
        }

        // 3. (RocketMQ) 如果是PASS，那么发送消息给rocketmq，让用户中心去消费，并为发布人添加积分
        if (AuditStatusEnum.PASS.equals(auditDTO.getAuditStatusEnum())) {
            // 发送半消息 ...
            String transactionId = UUID.randomUUID().toString();
            this.rocketMQTemplate.sendMessageInTransaction(
                    "tx-add-bonus-group",
                    "add-bonus",
                    MessageBuilder
                            .withPayload(
                                    UserAddBonusMsgDTO.builder()
                                            .userId(share.getUserId())
                                            .bonus(50)
                                            .build()
                            )
                            // header 有很大用处 ...
                            .setHeader(RocketMQHeaders.TRANSACTION_ID, transactionId)
                            .setHeader("share_id", id)
                            .build(),
                    // arg 有很大的用处 : 传参...
                    auditDTO
            );
        } else {
            // 审核不通过 操作数据库记录
            this.auditByIdInDB(id, auditDTO);
        }

        return share;
    }

    // 将 2.审核资源 步骤封装出代码
    @Transactional(rollbackFor = Exception.class)
    public void auditByIdInDB(Integer id, ShareAuditDTO auditDTO) {
        Share share = Share.builder()
                .id(id)
                .auditStatus(auditDTO.getAuditStatusEnum().toString())
                .reason(auditDTO.getReason())
                .build();
        shareMapper.updateByPrimaryKeySelective(share);


    }

    // 增加一条记录日志的方法
    @Transactional(rollbackFor = Exception.class)
    public void auditByIdWithRocketMqLog(Integer id, ShareAuditDTO auditDTO, String transactionId) {
        this.auditByIdInDB(id, auditDTO);

        this.rocketmqTransactionLogMapper.insertSelective(
                RocketmqTransactionLog.builder()
                        .transactionId(transactionId)
                        .log("审核分享...")
                        .build()
        );
    }

    // 判断是否存在审核的记录日志
    public String selectHasTransactionLog(String transactionId) {
        RocketmqTransactionLog rocketmqTransactionLog = this.rocketmqTransactionLogMapper.selectOne(
                RocketmqTransactionLog.builder()
                        .transactionId(transactionId)
                        .build()
        );
        if (rocketmqTransactionLog != null) {
            return "HAS";
        }

        return "UN_HAS";
    }
}
