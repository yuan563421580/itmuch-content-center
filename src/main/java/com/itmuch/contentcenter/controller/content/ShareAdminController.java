package com.itmuch.contentcenter.controller.content;

import com.itmuch.contentcenter.domain.dto.content.ShareAuditDTO;
import com.itmuch.contentcenter.domain.entity.content.Share;
import com.itmuch.contentcenter.service.content.ShareAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/shares")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShareAdminController {

    private final ShareAdminService shareAdminService;

    @PutMapping("/audit/{id}")
    public Share auditById(@PathVariable Integer id, @RequestBody ShareAuditDTO auditDTO) {
        // TODO 认证授权
        // return this.shareService.auditById(id, auditDTO);

        // 实现 RocketMQ 分布式事务的逻辑
        //return this.shareService.auditByIdTrans(id, auditDTO);

        // Stream + RocketMQ 实现分布式事务的逻辑
        return this.shareAdminService.auditByIdStreamMqTrans(id, auditDTO);
    }

}
