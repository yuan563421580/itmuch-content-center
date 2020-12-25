package com.itmuch.contentcenter.rocketmq;

import com.alibaba.fastjson.JSON;
import com.itmuch.contentcenter.domain.dto.content.ShareAuditDTO;
import com.itmuch.contentcenter.service.content.ShareAdminService;
import com.itmuch.contentcenter.service.content.ShareService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * 继承 RocketMQLocalTransactionListener
 * 使用注解 @RocketMQTransactionListener :
 *  txProducerGroup 一定要与 spring.cloud.stream.rocketmq.bindings.my-mq-output.producer.group 配置的值保持一致
 */
@RocketMQTransactionListener(txProducerGroup = "stream-tx-add-bonus-group")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NewAddBonusTransactionListener implements RocketMQLocalTransactionListener {

    private final ShareAdminService shareAdminService;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object arg) {
        // 用来执行本地事务的接口

        // 注意 arg 值为空, dto对象转换成 JsonString 放在了 header 中

        // 获取 Message
        MessageHeaders headers = message.getHeaders();

        // Meaage 的 header 信息
        String transactionId = (String) headers.get(RocketMQHeaders.TRANSACTION_ID);
        Integer shareId = Integer.valueOf((String) headers.get("share_id"));
        // 获取 dto 的字符串
        String dtoString = (String) headers.get("dto");
        // 将字符串进行反序列化
        ShareAuditDTO shareAuditDTO = JSON.parseObject(dtoString, ShareAuditDTO.class);

        // 处理本地业务
        try {
            // shareService.auditByIdInDB(shareId, (ShareAuditDTO) o);
            shareAdminService.auditByIdWithRocketMqLog(shareId, shareAuditDTO, transactionId);
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        // 用来检查本地事务的接口

        MessageHeaders headers = message.getHeaders();
        String transactionId = (String) headers.get(RocketMQHeaders.TRANSACTION_ID);

        String flag = shareAdminService.selectHasTransactionLog(transactionId);
        if ("HAS".equals(flag)) {
            return RocketMQLocalTransactionState.COMMIT;
        }

        return RocketMQLocalTransactionState.ROLLBACK;
    }

}
