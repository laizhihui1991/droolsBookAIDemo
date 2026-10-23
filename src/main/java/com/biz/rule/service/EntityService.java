package com.biz.rule.service;

import com.biz.rule.common.exception.BizException;
import com.biz.rule.engine.compiler.DynamicEntityCompiler;
import com.biz.rule.engine.model.EntityModel;
import com.biz.rule.entity.RuleEntityDO;
import com.biz.rule.mapper.RuleEntityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务实体管理 Service
 * 负责实体CRUD、动态生成Java源码、为Drools编译提供实体源码
 */
@Slf4j
@Service
public class EntityService {

    @Autowired
    private RuleEntityMapper entityMapper;

    private final DynamicEntityCompiler compiler = new DynamicEntityCompiler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<RuleEntityDO> list() {
        return entityMapper.selectList(null);
    }

    public RuleEntityDO getById(Long id) {
        RuleEntityDO entity = entityMapper.selectById(id);
        if (entity == null) {
            throw new BizException("实体不存在: " + id);
        }
        return entity;
    }

    public Long save(RuleEntityDO entity) {
        if (entity.getId() == null) {
            entityMapper.insert(entity);
        } else {
            entityMapper.updateById(entity);
        }
        return entity.getId();
    }

    public void delete(Long id) {
        entityMapper.deleteById(id);
    }

    /**
     * 根据实体编码，生成 Java 源码字符串。
     * 这是供 Drools KieFileSystem 编译用的核心方法。
     */
    public String getEntityJavaSource(String entityCode) {
        RuleEntityDO entity = entityMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RuleEntityDO>()
                        .eq(RuleEntityDO::getEntityCode, entityCode));
        if (entity == null) {
            throw new BizException("实体不存在: " + entityCode);
        }
        try {
            EntityModel model = objectMapper.readValue(entity.getFieldJson(), EntityModel.class);
            if (model.getPackageName() == null) {
                model.setPackageName(entity.getPackageName());
            }
            return compiler.generateSource(model);
        } catch (Exception e) {
            throw new BizException("实体源码生成失败: " + e.getMessage());
        }
    }

    /**
     * 根据 DRL 的 imports 列表，自动加载需要的实体 Java 源码。
     * 例如 imports = ["com.biz.rule.entity.Order", "com.biz.rule.entity.User", "java.math.BigDecimal"]
     * 会到数据库查 Order、User 实体，生成 Java 源码返回。
     *
     * @return fullClassName -> javaSource
     */
    public Map<String, String> loadEntitySourcesByImports(List<String> imports) {
        Map<String, String> result = new HashMap<>();
        if (imports == null) {
            return result;
        }
        for (String imp : imports) {
            if (imp == null || imp.isEmpty()) {
                continue;
            }
            // 只处理 com.biz.rule.entity 包下的实体（其他JDK类不处理）
            if (!imp.startsWith("com.biz.rule.entity.")) {
                continue;
            }
            String className = imp.substring(imp.lastIndexOf('.') + 1);
            try {
                String source = getEntityJavaSource(className);
                result.put(imp, source);
                log.debug("加载实体源码: {}", imp);
            } catch (BizException e) {
                // 实体不存在，跳过（编译时Drools会报错提示用户）
                log.warn("import的实体在数据库中不存在: {}，将由Drools编译时提示", imp);
            }
        }
        return result;
    }
}
