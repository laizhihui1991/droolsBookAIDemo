package com.biz.rule.engine.check;

import com.biz.rule.engine.model.RuleModel;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 规则冲突与重复检测器
 * 一期实现：
 *   1. 规则名重复
 *   2. 条件约束完全相同（重复规则）
 *   3. 同字段 RHS 冲突赋值（粗粒度）
 *   4. salience 相同且条件高度重叠（优先级冲突）
 */
@Component
public class ConflictDetector {

    public List<ConflictItem> detect(RuleModel model) {
        List<ConflictItem> items = new ArrayList<>();
        if (model == null || model.getRules() == null) {
            return items;
        }
        List<RuleModel.RuleDef> rules = model.getRules();

        // 1. 名称重复
        Set<String> names = new HashSet<>();
        for (RuleModel.RuleDef r : rules) {
            if (!names.add(r.getName())) {
                items.add(new ConflictItem("HIGH", "规则名重复",
                        "规则 [" + r.getName() + "] 名称已存在", r.getName()));
            }
        }

        // 2. 两两比较
        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                RuleModel.RuleDef a = rules.get(i);
                RuleModel.RuleDef b = rules.get(j);
                checkDuplicate(a, b, items);
                checkSalienceConflict(a, b, items);
            }
        }
        return items;
    }

    private void checkDuplicate(RuleModel.RuleDef a, RuleModel.RuleDef b, List<ConflictItem> items) {
        String ca = constraintsKey(a);
        String cb = constraintsKey(b);
        if (ca.equals(cb) && !ca.isEmpty()) {
            items.add(new ConflictItem("HIGH", "重复规则",
                    "规则 [" + a.getName() + "] 与 [" + b.getName() + "] 条件完全相同",
                    a.getName() + "/" + b.getName()));
        }
    }

    private void checkSalienceConflict(RuleModel.RuleDef a, RuleModel.RuleDef b, List<ConflictItem> items) {
        Integer sa = a.getSalience() == null ? 0 : a.getSalience();
        Integer sb = b.getSalience() == null ? 0 : b.getSalience();
        if (sa.equals(sb)) {
            // 简单判断：条件约束交集超过一半即提示
            Set<String> saSet = new HashSet<>(constraintsList(a));
            Set<String> sbSet = new HashSet<>(constraintsList(b));
            int intersect = 0;
            for (String s : saSet) {
                if (sbSet.contains(s)) {
                    intersect++;
                }
            }
            int union = saSet.size() + sbSet.size() - intersect;
            if (union > 0 && (double) intersect / union >= 0.6) {
                items.add(new ConflictItem("MEDIUM", "优先级冲突",
                        "规则 [" + a.getName() + "] 与 [" + b.getName() + "] 条件高度重叠且 salience 相同(" + sa + ")",
                        a.getName() + "/" + b.getName()));
            }
        }
    }

    private String constraintsKey(RuleModel.RuleDef r) {
        return String.join("|", constraintsList(r));
    }

    private List<String> constraintsList(RuleModel.RuleDef r) {
        List<String> list = new ArrayList<>();
        // when 现在是 DRL 原文字符串，直接按行拆分做相似度比较
        if (r.getWhen() != null && !r.getWhen().isEmpty()) {
            for (String line : r.getWhen().split("\n")) {
                String t = line.trim();
                if (!t.isEmpty()) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    @Data
    public static class ConflictItem {
        private String level;
        private String type;
        private String message;
        private String relatedRules;

        public ConflictItem(String level, String type, String message, String relatedRules) {
            this.level = level;
            this.type = type;
            this.message = message;
            this.relatedRules = relatedRules;
        }
    }
}
