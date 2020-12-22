package com.itmuch.contentcenter.domain.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测试 RocketMQ 的消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddBonusMsgDTO {

    /**
     * 为谁加积分
     */
    private Integer userId;
    /**
     * 加多少积分
     */
    private Integer bonus;

}
