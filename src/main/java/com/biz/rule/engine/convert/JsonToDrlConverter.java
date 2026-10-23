package com.biz.rule.engine.convert;

import com.biz.rule.common.exception.BizException;
import com.biz.rule.engine.model.RuleModel;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 规则JSON模型 → DRL 字符串转换器
 * 输出标准 Drools DRL：package / import / global / function / query / rule
 */
@Component
public class JsonToDrlConverter {

    private static final String INDENT = "    ";

    public String convert(RuleModel model) {
        if (model == null || StringUtils.isEmpty(model.getPackageName())) {
            throw new BizException("规则JSON缺少packageName");
        }
        StringBuilder sb = new StringBuilder(4096);

        // 1. package
        sb.append("package ").append(model.getPackageName()).append(";\n\n");

        // 2. imports
        if (!CollectionUtils.isEmpty(model.getImports())) {
            for (String imp : model.getImports()) {
                if (!StringUtils.isEmpty(imp)) {
                    sb.append("import ").append(imp).append(";\n");
                }
            }
            sb.append("\n");
        }

        // 3. globals
        if (!CollectionUtils.isEmpty(model.getGlobals())) {
            for (RuleModel.GlobalDef g : model.getGlobals()) {
                sb.append("global ").append(g.getType()).append(" ").append(g.getName()).append(";\n");
            }
            sb.append("\n");
        }

        // 4. functions
        if (!CollectionUtils.isEmpty(model.getFunctions())) {
            for (RuleModel.FunctionDef f : model.getFunctions()) {
                appendFunction(sb, f);
                sb.append("\n");
            }
        }

        // 5. queries
        if (!CollectionUtils.isEmpty(model.getQueries())) {
            for (RuleModel.QueryDef q : model.getQueries()) {
                appendQuery(sb, q);
                sb.append("\n");
            }
        }

        // 6. rules
        if (!CollectionUtils.isEmpty(model.getRules())) {
            for (RuleModel.RuleDef r : model.getRules()) {
                appendRule(sb, r);
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private void appendFunction(StringBuilder sb, RuleModel.FunctionDef f) {
        sb.append("function ").append(f.getReturnType() == null ? "void" : f.getReturnType())
                .append(" ").append(f.getName()).append("(");
        if (!CollectionUtils.isEmpty(f.getParams())) {
            String params = f.getParams().stream()
                    .map(p -> p.getType() + " " + p.getName())
                    .collect(Collectors.joining(", "));
            sb.append(params);
        }
        sb.append(") {\n");
        sb.append(INDENT).append(f.getBody() == null ? "" : f.getBody()).append("\n");
        sb.append("}\n");
    }

    private void appendQuery(StringBuilder sb, RuleModel.QueryDef q) {
        sb.append("query ").append(q.getName()).append("(");
        if (!CollectionUtils.isEmpty(q.getParams())) {
            String params = q.getParams().stream()
                    .map(p -> p.getType() + " " + p.getName())
                    .collect(Collectors.joining(", "));
            sb.append(params);
        }
        sb.append(")\n");
        sb.append(INDENT).append(q.getBody() == null ? "" : q.getBody()).append("\n");
        sb.append("end\n");
    }

    private void appendRule(StringBuilder sb, RuleModel.RuleDef r) {
        if (StringUtils.isEmpty(r.getName())) {
            throw new BizException("规则缺少name");
        }
        sb.append("rule \"").append(r.getName()).append("\"\n");
        if (r.getSalience() != null) {
            sb.append(INDENT).append("salience ").append(r.getSalience()).append("\n");
        }
        sb.append(INDENT).append("enabled ").append(r.getEnabled() == null || r.getEnabled()).append("\n");
        sb.append(INDENT).append("no-loop ").append(Boolean.TRUE.equals(r.getNoLoop())).append("\n");

        // when：直接输出 DRL 原文（多行字符串）
        sb.append("when\n");
        if (!StringUtils.isEmpty(r.getWhen())) {
            // 给每一行加缩进
            String[] lines = r.getWhen().split("\n");
            for (String line : lines) {
                sb.append(INDENT).append(line.trim()).append("\n");
            }
        }

        // then：直接输出 DRL 原文（支持 modify/insert/update 等多行块）
        sb.append("then\n");
        if (!StringUtils.isEmpty(r.getThen())) {
            String[] lines = r.getThen().split("\n");
            for (String line : lines) {
                sb.append(INDENT).append(line.trim()).append("\n");
            }
        }
        sb.append("end\n");
    }
}
