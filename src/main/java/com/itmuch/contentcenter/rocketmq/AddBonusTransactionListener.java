package com.itmuch.contentcenter.rocketmq;

import com.itmuch.contentcenter.domain.dto.content.ShareAuditDTO;
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
 * 使用注解 @RocketMQTransactionListener : txProducerGroup 一定要与 sendMessageInTransaction() 方法的值保持一致
 */
@RocketMQTransactionListener(txProducerGroup = "tx-add-bonus-group")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AddBonusTransactionListener implements RocketMQLocalTransactionListener {

    private final ShareService shareService;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object o) {
        // 用来执行本地事务的接口

        // 获取 Message
        MessageHeaders headers = message.getHeaders();

        // Meaage 的 header 信息
        String transactionId = (String) headers.get(RocketMQHeaders.TRANSACTION_ID);
        Integer shareId = Integer.valueOf((String) headers.get("share_id"));

        // 处理本地业务
        try {
            // shareService.auditByIdInDB(shareId, (ShareAuditDTO) o);
            shareService.auditByIdWithRocketMqLog(shareId, (ShareAuditDTO) o, transactionId);
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

        String flag = shareService.selectHasTransactionLog(transactionId);
        if ("HAS".equals(flag)) {
            return RocketMQLocalTransactionState.COMMIT;
        }

        return RocketMQLocalTransactionState.ROLLBACK;
    }

}
