package com.biz.rule.controller;

import com.biz.rule.common.result.Result;
import com.biz.rule.service.LlmTestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/llm/testcase")
public class LlmTestCaseController {

    @Autowired
    private LlmTestCaseService llmTestCaseService;

    @PostMapping("/generate")
    public Result<List<Map<String, Object>>> generate(@RequestBody Map<String, String> body) {
        String scenario = body.get("scenario");
        return Result.success(llmTestCaseService.generateTestFacts(scenario));
    }
}
