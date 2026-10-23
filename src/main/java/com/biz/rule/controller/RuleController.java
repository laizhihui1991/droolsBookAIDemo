package com.biz.rule.controller;

import com.biz.rule.common.result.Result;
import com.biz.rule.engine.model.RuleModel;
import com.biz.rule.entity.RuleInfoDO;
import com.biz.rule.entity.RuleVersionDO;
import com.biz.rule.service.RuleService;
import com.biz.rule.service.ValidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 规则管理
 */
@RestController
@RequestMapping("/api/rule")
public class RuleController {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private ValidateService validateService;

    @GetMapping("/list")
    public Result<List<RuleInfoDO>> list() {
        return Result.success(ruleService.list());
    }

    @GetMapping("/{id}")
    public Result<RuleInfoDO> get(@PathVariable Long id) {
        return Result.success(ruleService.getInfo(id));
    }

    @PostMapping("/create")
    public Result<Long> create(@RequestBody RuleInfoDO info) {
        return Result.success(ruleService.createRule(info));
    }

    /**
     * 保存版本（JSON + 关联实体）
     */
    @PostMapping("/{id}/version")
    public Result<Integer> saveVersion(@PathVariable Long id,
                                      @RequestBody SaveVersionReq req) {
        Integer v = ruleService.saveVersion(id, req.getRuleJson(), req.getEntityRefs(),
                req.getUser(), req.getRemark());
        return Result.success(v);
    }

    @GetMapping("/{id}/versions")
    public Result<List<RuleVersionDO>> versions(@PathVariable Long id) {
        return Result.success(ruleService.listVersions(id));
    }

    @GetMapping("/{id}/version/{version}")
    public Result<RuleVersionDO> version(@PathVariable Long id, @PathVariable Integer version) {
        return Result.success(ruleService.getVersion(id, version));
    }

    /**
     * 实时校验（语法 + 冲突）
     */
    @PostMapping("/validate")
    public Result<Map<String, Object>> validate(@RequestBody String ruleJson) {
        return Result.success(validateService.validate(ruleJson));
    }

    /**
     * DRL 预览
     */
    @PostMapping("/preview-drl")
    public Result<String> previewDrl(@RequestBody RuleModel model) {
        return Result.success(ruleService.previewDrl(model));
    }

    public static class SaveVersionReq {
        private String ruleJson;
        private String entityRefs;
        private String user;
        private String remark;

        public String getRuleJson() { return ruleJson; }
        public void setRuleJson(String ruleJson) { this.ruleJson = ruleJson; }
        public String getEntityRefs() { return entityRefs; }
        public void setEntityRefs(String entityRefs) { this.entityRefs = entityRefs; }
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
