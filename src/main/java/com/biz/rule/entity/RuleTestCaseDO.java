package com.biz.rule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试用例表
 */
@Data
@TableName("rule_test_case")
public class RuleTestCaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private Integer ruleVersion;

    private String caseName;

    /** 测试事实数据JSON */
    private String factJson;

    private String expectResult;

    private String actualResult;

    /** 0未执行 1通过 2不通过 */
    private Integer passStatus;

    private LocalDateTime createTime;

    private LocalDateTime executeTime;
}
