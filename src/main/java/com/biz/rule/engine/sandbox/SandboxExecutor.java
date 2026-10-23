package com.biz.rule.engine.sandbox;

import com.biz.rule.common.exception.BizException;
import com.biz.rule.engine.DroolsEngineHelper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 沙箱执行器
 * 传入测试事实JSON → 转Java对象 → 执行规则 → 返回命中数与事实快照
 */
@Slf4j
@Component
public class SandboxExecutor {

    @Autowired
    private DroolsEngineHelper engineHelper;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 执行沙箱测试
     *
     * @param drlContent     DRL文本
     * @param entitySources  动态实体Java源码（fullClassName -> sourceCode）
     * @param factJsonList   事实JSON列表（每个元素是一个Map，需含$entityClass指定全类名）
     */
    public SandboxResult run(String drlContent,
                             Map<String, String> entitySources,
                             List<Map<String, Object>> factJsonList) {
        KieContainer container = engineHelper.buildKieContainer(drlContent, entitySources);
        List<Object> facts = new ArrayList<>();

        if (factJsonList != null) {
            for (Map<String, Object> factMap : factJsonList) {
                String className = (String) factMap.get("$entityClass");
                if (className == null) {
                    throw new BizException("事实缺少$entityClass字段，无法映射实体类");
                }
                try {
                    // 从KieContainer的KieBase里加载实体类
                    Class<?> clazz = container.getClassLoader().loadClass(className);
                    Object fact = objectMapper.convertValue(factMap, clazz);
                    facts.add(fact);
                } catch (ClassNotFoundException e) {
                    throw new BizException("未找到实体类: " + className + "，请先在实体管理中定义");
                }
            }
        }

        String before = snapshot(facts);
        int fired = engineHelper.fireRules(container, facts);
        String after = snapshot(facts);

        SandboxResult result = new SandboxResult();
        result.setFiredRules(fired);
        result.setBeforeSnapshot(before);
        result.setAfterSnapshot(after);
        return result;
    }

    private String snapshot(List<Object> facts) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(facts);
        } catch (Exception e) {
            return facts.toString();
        }
    }

    @Data
    public static class SandboxResult {
        private int firedRules;
        private String beforeSnapshot;
        private String afterSnapshot;
    }
}
