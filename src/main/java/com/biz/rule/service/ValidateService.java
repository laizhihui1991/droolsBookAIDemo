package com.biz.rule.service;

import com.biz.rule.engine.DroolsEngineHelper;
import com.biz.rule.engine.model.RuleModel;
import com.biz.rule.engine.check.ConflictDetector;
import com.biz.rule.engine.convert.JsonToDrlConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则校验 Service
 * 语法校验 + 命名规范校验 + 冲突检测
 */
@Slf4j
@Service
public class ValidateService {

    @Autowired
    private JsonToDrlConverter converter;

    @Autowired
    private DroolsEngineHelper engineHelper;

    @Autowired
    private ConflictDetector conflictDetector;

    @Autowired
    private EntityService entityService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> validate(String ruleJson) {
        Map<String, Object> result = new HashMap<>();
        try {
            RuleModel model = objectMapper.readValue(ruleJson, RuleModel.class);

            // 1. JSON -> DRL
            String drl = converter.convert(model);
            result.put("drl", drl);

            // 预置实体已在classpath，直接校验，无需从数据库加载
            String syntaxError = engineHelper.validateSyntax(drl, null);
            result.put("syntaxOk", syntaxError == null);
            result.put("syntaxError", syntaxError);

            // 4. 冲突检测
            List<ConflictDetector.ConflictItem> conflicts = conflictDetector.detect(model);
            result.put("conflicts", conflicts);
            result.put("conflictCount", conflicts.size());

            // 5. 命名规范检查
            result.put("namingIssues", checkNaming(model));
        } catch (Exception e) {
            result.put("syntaxOk", false);
            result.put("syntaxError", "JSON解析失败: " + e.getMessage());
        }
        return result;
    }

    private List<String> checkNaming(RuleModel model) {
        List<String> issues = new java.util.ArrayList<>();
        if (model.getRules() != null) {
            for (RuleModel.RuleDef r : model.getRules()) {
                if (r.getName() != null && !r.getName().matches("^[a-z][a-z0-9_]*$")) {
                    issues.add("规则名 [" + r.getName() + "] 不符合小写+下划线规范");
                }
            }
        }
        return issues;
    }
}
