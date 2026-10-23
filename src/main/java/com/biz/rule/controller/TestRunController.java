package com.biz.rule.controller;

import com.biz.rule.common.result.Result;
import com.biz.rule.engine.convert.JsonToDrlConverter;
import com.biz.rule.engine.model.RuleModel;
import com.biz.rule.engine.sandbox.SandboxExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 直接执行沙箱测试（不依赖数据库测试用例）
 */
@RestController
@RequestMapping("/api/testcase")
public class TestRunController {

    @Autowired
    private JsonToDrlConverter converter;

    @Autowired
    private SandboxExecutor sandboxExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/run-direct")
    public Result<SandboxExecutor.SandboxResult> runDirect(@RequestBody Map<String, Object> body) {
        try {
            String ruleJson = (String) body.get("ruleJson");
            List<Map<String, Object>> facts = (List<Map<String, Object>>) body.get("facts");
            RuleModel model = objectMapper.readValue(ruleJson, RuleModel.class);
            String drl = converter.convert(model);
            // 预置实体已在classpath，直接执行
            SandboxExecutor.SandboxResult result = sandboxExecutor.run(drl, null, facts);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("执行失败: " + e.getMessage());
        }
    }
}
