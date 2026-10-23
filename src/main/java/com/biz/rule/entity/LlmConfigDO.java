package com.biz.rule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM配置表
 */
@Data
@TableName("llm_config")
public class LlmConfigDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configCode;

    /** 厂商：zhipu / 其他 */
    private String provider;

    private String modelName;

    private String apiKey;

    private String apiUrl;

    /** 超时秒数 */
    private Integer timeout;

    private Integer enable;

    private LocalDateTime createTime;
}
