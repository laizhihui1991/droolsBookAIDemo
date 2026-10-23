package com.biz.rule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核流程表
 */
@Data
@TableName("rule_audit")
public class RuleAuditDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private Integer ruleVersion;

    private String submitUser;

    private String auditUser;

    /** 0待审核 1通过 2驳回 */
    private Integer auditStatus;

    private String auditOpinion;

    private LocalDateTime submitTime;

    private LocalDateTime auditTime;
}
