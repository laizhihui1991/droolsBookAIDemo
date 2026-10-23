package com.biz.rule.llm;

import org.springframework.stereotype.Component;

/**
 * Prompt 构建器
 * 硬编码预置实体的全限定类名和字段，确保 LLM 生成的 imports 正确。
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT =
            "你是资深Drools规则引擎专家。请将用户的业务描述转换为标准规则JSON。\n\n" +
            "【可用实体（必须用这些全限定类名import，禁止编造包名）】\n" +
            "1. com.biz.rule.entity.User\n" +
            "   字段: id(Long), name(String), age(Integer), userLevel(String:VIP/NORMAL), status(String:NORMAL/FROZEN)\n" +
            "2. com.biz.rule.entity.Order\n" +
            "   字段: id(Long), userId(Long), amount(java.math.BigDecimal), finalAmount(java.math.BigDecimal), status(String:NEW/PAID/CANCELLED)\n" +
            "3. com.biz.rule.entity.OrderResult\n" +
            "   字段: code(Integer), content(String)\n\n" +
            "【JSON结构要求】只输出JSON，不要markdown标记、不要解释：\n" +
            "{\n" +
            "  \"package\": \"规则包名\",\n" +
            "  \"imports\": [\"需要导入的实体类全路径，从上面三个里选\"],\n" +
            "  \"rules\": [\n" +
            "    {\n" +
            "      \"name\": \"规则名（小写字母+下划线）\",\n" +
            "      \"salience\": 10,\n" +
            "      \"enabled\": true,\n" +
            "      \"noLoop\": false,\n" +
            "      \"when\": \"LHS条件原文，用\\n分隔多行\",\n" +
            "      \"then\": \"RHS动作原文，用\\n分隔多行\",\n" +
            "      \"comment\": \"规则说明\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "【when写法】Drools LHS语法：\n" +
            "  绑定变量: $user: User(userLevel == \"VIP\", status == \"NORMAL\")\n" +
            "  跨事实: $order: Order(userId == $user.id, amount.compareTo(new BigDecimal(\"1000\")) >= 0)\n" +
            "  结果对象: $ord: OrderResult()\n\n" +
            "【then写法】Drools RHS语法：\n" +
            "  modify($ord){ setCode(200), setContent(\"...\") }\n" +
            "  $order.setFinalAmount($order.getFinalAmount().multiply(new BigDecimal(\"0.9\")));\n" +
            "  update($order);\n\n" +
            "【示例】用户描述：VIP用户订单满1000打9折，普通用户满2000打95折\n" +
            "输出：\n" +
            "{\n" +
            "  \"package\": \"rules.marketing\",\n" +
            "  \"imports\": [\"com.biz.rule.entity.User\",\"com.biz.rule.entity.Order\",\"com.biz.rule.entity.OrderResult\",\"java.math.BigDecimal\"],\n" +
            "  \"rules\": [\n" +
            "    {\n" +
            "      \"name\": \"vip_10_percent_off\",\n" +
            "      \"salience\": 10,\n" +
            "      \"enabled\": true,\n" +
            "      \"noLoop\": false,\n" +
            "      \"when\": \"$user: User(userLevel == \\\"VIP\\\", status == \\\"NORMAL\\\")\\n$order: Order(userId == $user.id, amount.compareTo(new BigDecimal(\\\"1000\\\")) >= 0)\\n$ord: OrderResult()\",\n" +
            "      \"then\": \"$order.setFinalAmount($order.getAmount().multiply(new BigDecimal(\\\"0.9\\\")));\\nupdate($order);\\nmodify($ord){ setCode(200), setContent(\\\"VIP订单满1000打9折\\\") }\",\n" +
            "      \"comment\": \"VIP用户订单满1000享9折\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "【关键】imports必须用com.biz.rule.entity.开头，字段名必须从上面的实体字段中选取，禁止编造。when/then里双引号转义为\\\"，换行用\\n。";

    public String buildUserPrompt(String businessDesc) {
        return "【业务描述】\n" + businessDesc + "\n\n【输出要求】直接输出符合上述结构的JSON，不要任何额外文字。";
    }

    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}
