package com.biz.rule.controller;

import com.biz.rule.common.result.Result;
import com.biz.rule.service.LlmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LLM 智能规则生成
 */
@RestController
@RequestMapping("/api/llm")
public class LlmController {

    @Autowired
    private LlmService llmService;

    @PostMapping("/generate")
    public Result<String> generate(@RequestBody GenerateReq req) {
        String json = llmService.generateRuleJson(
                req.getPackageName(),
                req.getEntities(),
                req.getBusinessDesc(),
                req.getConfigCode());
        return Result.success(json);
    }

    public static class GenerateReq {
        private String packageName;
        private List<com.biz.rule.engine.model.EntityModel> entities;
        private String businessDesc;
        private String configCode;

        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        public List<com.biz.rule.engine.model.EntityModel> getEntities() { return entities; }
        public void setEntities(List<com.biz.rule.engine.model.EntityModel> entities) { this.entities = entities; }
        public String getBusinessDesc() { return businessDesc; }
        public void setBusinessDesc(String businessDesc) { this.businessDesc = businessDesc; }
        public String getConfigCode() { return configCode; }
        public void setConfigCode(String configCode) { this.configCode = configCode; }
    }
}
