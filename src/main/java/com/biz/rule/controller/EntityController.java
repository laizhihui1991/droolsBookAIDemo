package com.biz.rule.controller;

import com.biz.rule.common.result.Result;
import com.biz.rule.entity.RuleEntityDO;
import com.biz.rule.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务实体管理
 */
@RestController
@RequestMapping("/api/entity")
public class EntityController {

    @Autowired
    private EntityService entityService;

    @GetMapping("/list")
    public Result<List<RuleEntityDO>> list() {
        return Result.success(entityService.list());
    }

    @GetMapping("/{id}")
    public Result<RuleEntityDO> get(@PathVariable Long id) {
        return Result.success(entityService.getById(id));
    }

    @PostMapping("/save")
    public Result<Long> save(@RequestBody RuleEntityDO entity) {
        return Result.success(entityService.save(entity));
    }

    @PostMapping("/source/{entityCode}")
    public Result<String> source(@PathVariable String entityCode) {
        return Result.success(entityService.getEntityJavaSource(entityCode));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        entityService.delete(id);
        return Result.success();
    }
}
