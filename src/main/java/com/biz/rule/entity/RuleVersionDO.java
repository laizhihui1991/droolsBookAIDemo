package com.biz.rule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则版本表
 */
@Data
@TableName("rule_version")
public class RuleVersionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private Integer version;

    /** 标准规则JSON */
    private String ruleJson;

    /** 生成的DRL代码 */
    private String ruleDrl;

    private String createUser;

    private LocalDateTime createTime;

    private String remark;
}
