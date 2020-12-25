package com.itmuch.contentcenter.service.content;

import com.alibaba.fastjson.JSON;
import com.itmuch.contentcenter.dao.content.ShareMapper;
import com.itmuch.contentcenter.dao.messaging.RocketmqTransactionLogMapper;
import com.itmuch.contentcenter.domain.dto.content.ShareAuditDTO;
import com.itmuch.contentcenter.domain.dto.messaging.UserAddBonusMsgDTO;
import com.itmuch.contentcenter.domain.entity.content.Share;
import com.itmuch.contentcenter.domain.entity.messaging.RocketmqTransactionLog;
import com.itmuch.contentcenter.domain.enums.AuditStatusEnum;
import com.itmuch.contentcenter.rocketmq.MyMqSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShareAdminService {

    private final ShareMapper shareMapper;

    //private final RocketMQTemplate rocketMQTemplate;

    private final RocketmqTransactionLogMapper rocketmqTransactionLogMapper;

    private final MyMqSource myMqSource;

    //transactional
    /*@Transactional(rollbackFor = Exception.class)
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
    }*/

    // 分布式事务实现逻辑 ：RocketMQ 发送半消息
    // 不需要 @Transactional ，因为依赖了 RocketMQ 的分布式事务
    /*public Share auditByIdTrans(Integer id, ShareAuditDTO auditDTO) {
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
    }*/

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

    // 分布式事务实现逻辑 ：Stream + RocketMQ 发送半消息
    public Share auditByIdStreamMqTrans(Integer id, ShareAuditDTO auditDTO) {
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

            this.myMqSource.output()
                    .send(
                            MessageBuilder
                                    .withPayload(
                                            UserAddBonusMsgDTO.builder()
                                                    .userId(share.getUserId())
                                                    .bonus(50)
                                                    .build()
                                    )
                                    // header 有很大用处 ... header只能传字符串
                                    .setHeader(RocketMQHeaders.TRANSACTION_ID, transactionId)
                                    .setHeader("share_id", id)
                                    // 将 arg 参数转换成JSON字符串后放在 header 中传，因为该方法不支持
                                    .setHeader("dto", JSON.toJSONString(auditDTO))
                                    .build()
                    );

        } else {
            // 审核不通过 操作数据库记录
            this.auditByIdInDB(id, auditDTO);
        }

        return share;
    }

}
