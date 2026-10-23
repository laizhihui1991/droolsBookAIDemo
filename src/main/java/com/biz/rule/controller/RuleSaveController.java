package com.biz.rule.controller;

import com.biz.rule.common.result.Result;
import com.biz.rule.engine.convert.JsonToDrlConverter;
import com.biz.rule.engine.model.RuleModel;
import com.biz.rule.entity.RuleInfoDO;
import com.biz.rule.entity.RuleVersionDO;
import com.biz.rule.mapper.RuleInfoMapper;
import com.biz.rule.mapper.RuleVersionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 直接保存规则JSON到数据库（简化版，不搞版本审核流程）
 */
@RestController
@RequestMapping("/api/rule")
public class RuleSaveController {

    @Autowired
    private RuleInfoMapper infoMapper;
    @Autowired
    private RuleVersionMapper versionMapper;
    @Autowired
    private JsonToDrlConverter converter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/save-direct")
    public Result<Long> saveDirect(@RequestBody Map<String, String> body) {
        try {
            String ruleJson = body.get("ruleJson");
            String remark = body.get("remark");
            RuleModel model = objectMapper.readValue(ruleJson, RuleModel.class);
            String drl = converter.convert(model);

            // 找第一条规则名作为规则编码
            String ruleName = (model.getRules() != null && !model.getRules().isEmpty())
                    ? model.getRules().get(0).getName() : "rule_" + System.currentTimeMillis();

            // 查或建规则主表
            RuleInfoDO info = infoMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RuleInfoDO>()
                            .eq(RuleInfoDO::getRuleCode, ruleName));
            if (info == null) {
                info = new RuleInfoDO();
                info.setRuleCode(ruleName);
                info.setRuleName(ruleName);
                info.setRuleType("marketing");
                info.setPackageName(model.getPackageName());
                info.setCurrentVersion(1);
                info.setStatus(2); // 已发布
                info.setCreateUser("llm");
                info.setCreateTime(LocalDateTime.now());
                info.setUpdateTime(LocalDateTime.now());
                infoMapper.insert(info);
            } else {
                info.setCurrentVersion(info.getCurrentVersion() + 1);
                info.setUpdateTime(LocalDateTime.now());
                infoMapper.updateById(info);
            }

            // 存版本
            RuleVersionDO v = new RuleVersionDO();
            v.setRuleId(info.getId());
            v.setVersion(info.getCurrentVersion());
            v.setRuleJson(ruleJson);
            v.setRuleDrl(drl);
            v.setCreateUser("llm");
            v.setCreateTime(LocalDateTime.now());
            v.setRemark(remark);
            versionMapper.insert(v);

            return Result.success(info.getId());
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
    }
}
