package com.biz.rule.engine;

import com.biz.rule.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.drools.compiler.kproject.ReleaseIdImpl;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Drools 引擎核心工具类
 * 动态构建 KieContainer / KieSession，执行规则，捕获编译与执行结果。
 */
@Slf4j
@Component
public class DroolsEngineHelper {

    /**
     * 根据 DRL 字符串 + 动态实体 Java 源码构建 KieContainer。
     *
     * 关键：把实体的 .java 源码写入 KieFileSystem，KieBuilder 会先编译 Java 再编译 DRL，
     * 这样 DRL 里 import com.biz.rule.entity.Order 就能被正确解析。
     *
     * @param drlContent    完整 DRL
     * @param entitySources 动态实体 Java 源码（fullClassName -> sourceCode）
     */
    public KieContainer buildKieContainer(String drlContent, Map<String, String> entitySources) {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kfs = kieServices.newKieFileSystem();

        // 1. 写动态实体 Java 源码（KieBuilder 会先编译这些类）
        if (entitySources != null) {
            for (Map.Entry<String, String> e : entitySources.entrySet()) {
                String path = "src/main/java/" + e.getKey().replace('.', '/') + ".java";
                kfs.write(path, e.getValue());
                log.debug("写入实体源码: {}", path);
            }
        }

        // 2. 写 DRL
        kfs.write("src/main/resources/rules/temp-rule.drl", drlContent);

        ReleaseId releaseId = new ReleaseIdImpl("com.biz.rule", "temp-kjar", System.currentTimeMillis() + "");
        kfs.generateAndWritePomXML(releaseId);

        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            String msg = kieBuilder.getResults().getMessages(Message.Level.ERROR).toString();
            log.warn("DRL编译错误: {}", msg);
            throw new RuleCompileException("DRL编译失败: " + msg);
        }
        return kieServices.newKieContainer(releaseId);
    }

    /**
     * 执行规则
     */
    public int fireRules(KieContainer container, List<Object> facts) {
        KieSession session = container.newKieSession();
        try {
            if (facts != null) {
                facts.forEach(session::insert);
            }
            return session.fireAllRules();
        } finally {
            session.dispose();
        }
    }

    /**
     * 仅做语法编译校验，不执行
     */
    public String validateSyntax(String drlContent, Map<String, String> entitySources) {
        try {
            buildKieContainer(drlContent, entitySources);
            return null;
        } catch (RuleCompileException e) {
            return e.getMessage();
        }
    }

    /** 编译异常 */
    public static class RuleCompileException extends RuntimeException {
        public RuleCompileException(String msg) { super(msg); }
    }
}
