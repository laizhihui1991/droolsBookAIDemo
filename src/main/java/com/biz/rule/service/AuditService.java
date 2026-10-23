package com.biz.rule.service;

import com.biz.rule.common.exception.BizException;
import com.biz.rule.entity.RuleAuditDO;
import com.biz.rule.entity.RuleInfoDO;
import com.biz.rule.mapper.RuleAuditMapper;
import com.biz.rule.mapper.RuleInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核流程 Service
 * 状态：0草稿 → 1审核中 → 2已发布 / 3已停用
 */
@Service
public class AuditService {

    @Autowired
    private RuleAuditMapper auditMapper;

    @Autowired
    private RuleInfoMapper ruleInfoMapper;

    /**
     * 提交审核
     */
    public Long submit(Long ruleId, Integer version, String submitUser) {
        RuleInfoDO info = ruleInfoMapper.selectById(ruleId);
        if (info == null) {
            throw new BizException("规则不存在");
        }
        RuleAuditDO audit = new RuleAuditDO();
        audit.setRuleId(ruleId);
        audit.setRuleVersion(version);
        audit.setSubmitUser(submitUser);
        audit.setAuditStatus(0);
        audit.setSubmitTime(LocalDateTime.now());
        auditMapper.insert(audit);

        info.setStatus(1);
        ruleInfoMapper.updateById(info);
        return audit.getId();
    }

    /**
     * 审核操作
     * @param pass true=通过 false=驳回
     */
    public void audit(Long auditId, String auditUser, boolean pass, String opinion) {
        RuleAuditDO audit = auditMapper.selectById(auditId);
        if (audit == null) {
            throw new BizException("审核记录不存在");
        }
        audit.setAuditUser(auditUser);
        audit.setAuditStatus(pass ? 1 : 2);
        audit.setAuditOpinion(opinion);
        audit.setAuditTime(LocalDateTime.now());
        auditMapper.updateById(audit);

        RuleInfoDO info = ruleInfoMapper.selectById(audit.getRuleId());
        if (info != null) {
            info.setStatus(pass ? 2 : 0);
            ruleInfoMapper.updateById(info);
        }
    }

    public List<RuleAuditDO> listPending() {
        return auditMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RuleAuditDO>()
                        .eq(RuleAuditDO::getAuditStatus, 0)
                        .orderByDesc(RuleAuditDO::getSubmitTime));
    }
}
