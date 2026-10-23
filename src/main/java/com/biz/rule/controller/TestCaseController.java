package com.biz.rule.controller;

import com.biz.rule.common.result.Result;
import com.biz.rule.engine.sandbox.SandboxExecutor;
import com.biz.rule.entity.RuleTestCaseDO;
import com.biz.rule.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 测试用例
 */
@RestController
@RequestMapping("/api/testcase")
public class TestCaseController {

    @Autowired
    private TestCaseService testCaseService;

    @GetMapping("/list/{ruleId}")
    public Result<List<RuleTestCaseDO>> list(@PathVariable Long ruleId) {
        return Result.success(testCaseService.list(ruleId));
    }

    @PostMapping("/save")
    public Result<Long> save(@RequestBody RuleTestCaseDO testCase) {
        return Result.success(testCaseService.save(testCase));
    }

    @PostMapping("/execute/{id}")
    public Result<SandboxExecutor.SandboxResult> execute(@PathVariable Long id) {
        return Result.success(testCaseService.execute(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        testCaseService.delete(id);
        return Result.success();
    }
}
