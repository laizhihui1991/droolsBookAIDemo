package com.biz.rule.controller;

import com.biz.rule.common.result.Result;
import com.biz.rule.entity.RuleAuditDO;
import com.biz.rule.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审核流程
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @PostMapping("/submit")
    public Result<Long> submit(@RequestBody SubmitReq req) {
        return Result.success(auditService.submit(req.getRuleId(), req.getVersion(), req.getSubmitUser()));
    }

    @PostMapping("/{id}")
    public Result<Void> audit(@PathVariable Long id, @RequestBody AuditReq req) {
        auditService.audit(id, req.getAuditUser(), req.isPass(), req.getOpinion());
        return Result.success();
    }

    @GetMapping("/pending")
    public Result<List<RuleAuditDO>> pending() {
        return Result.success(auditService.listPending());
    }

    public static class SubmitReq {
        private Long ruleId;
        private Integer version;
        private String submitUser;

        public Long getRuleId() { return ruleId; }
        public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }
        public String getSubmitUser() { return submitUser; }
        public void setSubmitUser(String submitUser) { this.submitUser = submitUser; }
    }

    public static class AuditReq {
        private String auditUser;
        private boolean pass;
        private String opinion;

        public String getAuditUser() { return auditUser; }
        public void setAuditUser(String auditUser) { this.auditUser = auditUser; }
        public boolean isPass() { return pass; }
        public void setPass(boolean pass) { this.pass = pass; }
        public String getOpinion() { return opinion; }
        public void setOpinion(String opinion) { this.opinion = opinion; }
    }
}
