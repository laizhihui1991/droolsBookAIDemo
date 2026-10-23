package com.biz.rule.controller;

import com.biz.rule.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预置实体展示（User / Order / OrderResult）
 * 这些实体是真实Java类，Drools编译时直接可引用，无需动态创建。
 */
@RestController
@RequestMapping("/api/builtin")
public class BuiltinEntityController {

    @GetMapping("/entities")
    public Result<List<Map<String, Object>>> entities() {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> user = new HashMap<>();
        user.put("className", "User");
        user.put("packageName", "com.biz.rule.entity.User");
        user.put("comment", "用户");
        List<Map<String, String>> userFields = new ArrayList<>();
        userFields.add(field("id", "Long", "用户ID"));
        userFields.add(field("name", "String", "姓名"));
        userFields.add(field("age", "Integer", "年龄"));
        userFields.add(field("userLevel", "String", "等级：VIP/NORMAL"));
        userFields.add(field("status", "String", "状态：NORMAL/FROZEN"));
        user.put("fields", userFields);
        list.add(user);

        Map<String, Object> order = new HashMap<>();
        order.put("className", "Order");
        order.put("packageName", "com.biz.rule.entity.Order");
        order.put("comment", "订单");
        List<Map<String, String>> orderFields = new ArrayList<>();
        orderFields.add(field("id", "Long", "订单ID"));
        orderFields.add(field("userId", "Long", "用户ID"));
        orderFields.add(field("amount", "BigDecimal", "原始金额"));
        orderFields.add(field("finalAmount", "BigDecimal", "折后金额"));
        orderFields.add(field("status", "String", "状态：NEW/PAID/CANCELLED"));
        order.put("fields", orderFields);
        list.add(order);

        Map<String, Object> result = new HashMap<>();
        result.put("className", "OrderResult");
        result.put("packageName", "com.biz.rule.entity.OrderResult");
        result.put("comment", "规则执行结果");
        List<Map<String, String>> resultFields = new ArrayList<>();
        resultFields.add(field("code", "Integer", "返回码"));
        resultFields.add(field("content", "String", "返回内容"));
        result.put("fields", resultFields);
        list.add(result);

        return Result.success(list);
    }

    private Map<String, String> field(String name, String type, String comment) {
        Map<String, String> m = new HashMap<>();
        m.put("name", name);
        m.put("type", type);
        m.put("comment", comment);
        return m;
    }
}
