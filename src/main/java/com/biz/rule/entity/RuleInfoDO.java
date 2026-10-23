package com.biz.rule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则主表
 */
@Data
@TableName("rule_info")
public class RuleInfoDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleCode;

    private String ruleName;

    /** 规则分类：营销/风控/审批 */
    private String ruleType;

    private String packageName;

    /** 当前生效版本号 */
    private Integer currentVersion;

    /** 状态：0草稿 1审核中 2已发布 3已停用 */
    private Integer status;

    private String createUser;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
