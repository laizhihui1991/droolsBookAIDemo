package com.biz.rule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务实体表
 */
@Data
@TableName("rule_entity")
public class RuleEntityDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 实体唯一编码 */
    private String entityCode;

    /** 实体名称 */
    private String entityName;

    /** 包名 */
    private String packageName;

    /** 字段定义JSON */
    private String fieldJson;

    /** 额外导入包列表，逗号分隔 */
    private String importList;

    /** 状态：0草稿 1启用 2停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String remark;
}
