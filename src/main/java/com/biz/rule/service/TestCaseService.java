package com.biz.rule.service;

import com.biz.rule.common.exception.BizException;
import com.biz.rule.engine.model.RuleModel;
import com.biz.rule.engine.sandbox.SandboxExecutor;
import com.biz.rule.entity.RuleTestCaseDO;
import com.biz.rule.entity.RuleVersionDO;
import com.biz.rule.mapper.RuleTestCaseMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 测试用例 Service
 */
@Slf4j
@Service
public class TestCaseService {

    @Autowired
    private RuleTestCaseMapper testCaseMapper;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private SandboxExecutor sandboxExecutor;

    @Autowired
    private EntityService entityService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<RuleTestCaseDO> list(Long ruleId) {
        return testCaseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RuleTestCaseDO>()
                        .eq(RuleTestCaseDO::getRuleId, ruleId)
                        .orderByDesc(RuleTestCaseDO::getId));
    }

    public Long save(RuleTestCaseDO testCase) {
        testCaseMapper.insert(testCase);
        return testCase.getId();
    }

    public void delete(Long id) {
        testCaseMapper.deleteById(id);
    }

    /**
     * 执行测试用例：从规则版本JSON解析imports → 加载实体源码 → 沙箱执行
     */
    public SandboxExecutor.SandboxResult execute(Long testCaseId) {
        RuleTestCaseDO tc = testCaseMapper.selectById(testCaseId);
        if (tc == null) {
            throw new BizException("测试用例不存在: " + testCaseId);
        }
        RuleVersionDO version = ruleService.getVersion(tc.getRuleId(), tc.getRuleVersion());

        try {
            // 1. 从规则JSON解析imports，加载实体Java源码
            RuleModel model = objectMapper.readValue(version.getRuleJson(), RuleModel.class);
            Map<String, String> entitySources = entityService.loadEntitySourcesByImports(model.getImports());
            log.info("执行测试用例，加载了 {} 个实体源码", entitySources.size());

            // 2. 解析测试事实
            List<Map<String, Object>> facts = objectMapper.readValue(
                    tc.getFactJson(), new TypeReference<List<Map<String, Object>>>() {});

            // 3. 沙箱执行
            SandboxExecutor.SandboxResult result = sandboxExecutor.run(
                    version.getRuleDrl(), entitySources, facts);

            // 4. 回写结果
            tc.setActualResult(objectMapper.writeValueAsString(result));
            tc.setExecuteTime(LocalDateTime.now());
            if (tc.getExpectResult() != null && !tc.getExpectResult().isEmpty()) {
                tc.setPassStatus(result.getAfterSnapshot().contains(tc.getExpectResult()) ? 1 : 2);
            } else {
                tc.setPassStatus(1);
            }
            testCaseMapper.updateById(tc);
            return result;
        } catch (Exception e) {
            log.error("执行测试用例失败", e);
            tc.setPassStatus(2);
            tc.setActualResult("执行异常: " + e.getMessage());
            testCaseMapper.updateById(tc);
            throw new BizException("执行测试用例失败: " + e.getMessage());
        }
    }
}
