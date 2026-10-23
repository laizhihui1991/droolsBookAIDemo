package com.biz.rule.service;

import com.biz.rule.common.exception.BizException;
import com.biz.rule.engine.check.ConflictDetector;
import com.biz.rule.engine.convert.JsonToDrlConverter;
import com.biz.rule.engine.model.RuleModel;
import com.biz.rule.entity.RuleInfoDO;
import com.biz.rule.entity.RuleVersionDO;
import com.biz.rule.mapper.RuleInfoMapper;
import com.biz.rule.mapper.RuleVersionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则管理 Service
 * 负责规则元数据、版本、JSON↔DRL转换、冲突检测
 */
@Slf4j
@Service
public class RuleService {

    @Autowired
    private RuleInfoMapper ruleInfoMapper;

    @Autowired
    private RuleVersionMapper ruleVersionMapper;

    @Autowired
    private JsonToDrlConverter jsonToDrl;

    @Autowired
    private ConflictDetector conflictDetector;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<RuleInfoDO> list() {
        return ruleInfoMapper.selectList(null);
    }

    public RuleInfoDO getInfo(Long id) {
        RuleInfoDO info = ruleInfoMapper.selectById(id);
        if (info == null) {
            throw new BizException("规则不存在: " + id);
        }
        return info;
    }

    public Long createRule(RuleInfoDO info) {
        info.setStatus(0);
        info.setCurrentVersion(0);
        ruleInfoMapper.insert(info);
        return info.getId();
    }

    /**
     * 保存规则版本（JSON -> DRL -> 冲突检测 -> 入库）
     *
     * @param ruleId     规则ID
     * @param ruleJson   规则JSON字符串
     * @param entityRefs 关联实体编码
     * @param user       操作人
     * @param remark     版本备注
     * @return 新版本号
     */
    public Integer saveVersion(Long ruleId, String ruleJson, String entityRefs,
                               String user, String remark) {
        try {
            RuleModel model = objectMapper.readValue(ruleJson, RuleModel.class);

            // JSON -> DRL
            String drl = jsonToDrl.convert(model);

            // 冲突检测
            List<ConflictDetector.ConflictItem> conflicts = conflictDetector.detect(model);
            String conflictJson = objectMapper.writeValueAsString(conflicts);

            // 版本号自增
            RuleInfoDO info = getInfo(ruleId);
            int newVersion = (info.getCurrentVersion() == null ? 0 : info.getCurrentVersion()) + 1;

            RuleVersionDO version = new RuleVersionDO();
            version.setRuleId(ruleId);
            version.setVersion(newVersion);
            version.setRuleJson(ruleJson);
            version.setRuleDrl(drl);
            version.setCreateUser(user);
            version.setRemark(remark);
            version.setCreateTime(LocalDateTime.now());
            ruleVersionMapper.insert(version);

            // 更新主表当前版本
            info.setCurrentVersion(newVersion);
            ruleInfoMapper.updateById(info);

            log.info("规则版本保存成功: ruleId={}, version={}, 冲突项={}", ruleId, newVersion, conflicts.size());
            return newVersion;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存规则版本失败", e);
            throw new BizException("保存规则版本失败: " + e.getMessage());
        }
    }

    public RuleVersionDO getVersion(Long ruleId, Integer version) {
        RuleVersionDO v = ruleVersionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RuleVersionDO>()
                        .eq(RuleVersionDO::getRuleId, ruleId)
                        .eq(RuleVersionDO::getVersion, version));
        if (v == null) {
            throw new BizException("版本不存在: ruleId=" + ruleId + " v=" + version);
        }
        return v;
    }

    public List<RuleVersionDO> listVersions(Long ruleId) {
        return ruleVersionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RuleVersionDO>()
                        .eq(RuleVersionDO::getRuleId, ruleId)
                        .orderByDesc(RuleVersionDO::getVersion));
    }

    /**
     * 直接将 RuleModel 转 DRL（供前端实时预览）
     */
    public String previewDrl(RuleModel model) {
        return jsonToDrl.convert(model);
    }
}
