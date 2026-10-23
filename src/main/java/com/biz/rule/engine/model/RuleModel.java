package com.biz.rule.engine.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 规则JSON标准模型（核心中间层）
 *
 * 设计原则：when / then 直接使用 DRL 原文字符串，不做结构化拆分。
 * 原因：Drools LHS 包含绑定变量（$x: field == v）、跨事实引用、
 * eval(...)、accumulate 等复杂语法，强行拆成 JSON 数组会限制表达力，
 * 且 LLM 生成结构化数组极易出错。字符串方案让 LLM 直接输出 DRL 风格的条件/动作，
 * 既稳定又保留完整表达力。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleModel implements Serializable {

    /**
     * 规则包名。
     * JSON 中用 "package"（Drools 关键字），Java 字段用 packageName（避免关键字冲突）。
     * 同时兼容 "packageName" 字段名（前端源码面板可能输出）。
     */
    @JsonProperty("package")
    @JsonAlias("packageName")
    private String packageName;

    private List<String> imports;

    private List<GlobalDef> globals;

    private List<FunctionDef> functions;

    private List<QueryDef> queries;

    private List<RuleDef> rules;

    @Data
    public static class GlobalDef implements Serializable {
        private String name;
        private String type;
        private String comment;
    }

    @Data
    public static class FunctionDef implements Serializable {
        private String name;
        private String returnType;
        private List<ParamDef> params;
        private String body;
        private String comment;
    }

    @Data
    public static class QueryDef implements Serializable {
        private String name;
        private List<ParamDef> params;
        private String body;
        private String comment;
    }

    @Data
    public static class ParamDef implements Serializable {
        private String name;
        private String type;
    }

    @Data
    public static class RuleDef implements Serializable {
        /** 规则名 */
        private String name;
        /** 优先级 */
        private Integer salience;
        /** 是否启用 */
        private Boolean enabled;
        /** 是否 no-loop */
        private Boolean noLoop;
        /**
         * WHEN 条件块，DRL 原文（可多行）
         * 例：$user: User($userId: id == 10000, name == "张三", age == 30)
         *     $order: Order(userId == $user.id, amount > 100)
         */
        private String when;
        /**
         * THEN 动作块，DRL 原文（可多行，支持 modify/insert/update/System.out 等）
         * 例：modify($ord){ setCode(200), setContent("金额大于100") }
         */
        private String then;
        /** 规则注释 */
        private String comment;
    }
}
