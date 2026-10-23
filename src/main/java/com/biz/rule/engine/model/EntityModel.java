package com.biz.rule.engine.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 业务实体定义模型（对应方案 4.1.2）
 */
@Data
public class EntityModel implements Serializable {

    private String className;

    private String packageName;

    private List<String> imports;

    private List<FieldDef> fields;

    @Data
    public static class FieldDef implements Serializable {
        private String name;
        /** 类型：Long / String / BigDecimal / Date / Integer / Double 等 */
        private String type;
        private String comment;
        private String defaultValue;
    }
}
