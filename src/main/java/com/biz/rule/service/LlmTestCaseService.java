package com.biz.rule.service;

import com.biz.rule.common.exception.BizException;
import com.biz.rule.llm.ZhipuLlmClient;
import com.biz.rule.entity.LlmConfigDO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * LLM 生成测试用例：自然语言测试场景 → 事实JSON数组
 */
@Slf4j
@Service
public class LlmTestCaseService {

    @Autowired
    private LlmService llmService;

    @Autowired
    private ZhipuLlmClient llmClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
            "你是Drools测试用例生成助手。根据用户的自然语言测试场景，生成测试事实JSON数组。\n\n" +
            "可用实体：\n" +
            "- com.biz.rule.entity.User: id(Long), name(String), age(Integer), userLevel(String:VIP/NORMAL), status(String:NORMAL/FROZEN)\n" +
            "- com.biz.rule.entity.Order: id(Long), userId(Long), amount(用字符串表示BigDecimal,如\"1300.00\"), finalAmount(用字符串), status(String:NEW/PAID/CANCELLED)\n" +
            "- com.biz.rule.entity.OrderResult: code(Integer), content(String)\n\n" +
            "输出格式（只输出JSON数组，不要markdown）：\n" +
            "[\n" +
            "  {\"$entityClass\": \"com.biz.rule.entity.User\", \"id\":10001, \"name\":\"来家二少\", \"age\":30, \"userLevel\":\"VIP\", \"status\":\"NORMAL\"},\n" +
            "  {\"$entityClass\": \"com.biz.rule.entity.Order\", \"id\":1, \"userId\":10001, \"amount\":\"1300.00\", \"status\":\"NEW\"},\n" +
            "  {\"$entityClass\": \"com.biz.rule.entity.OrderResult\", \"code\":0, \"content\":\"\"},\n" +
            "  {\"$entityClass\": \"com.biz.rule.entity.User\", \"id\":10002, \"name\":\"上官无极\", \"age\":28, \"userLevel\":\"NORMAL\", \"status\":\"NORMAL\"},\n" +
            "  {\"$entityClass\": \"com.biz.rule.entity.Order\", \"id\":2, \"userId\":10002, \"amount\":\"3450.00\", \"status\":\"NEW\"},\n" +
            "  {\"$entityClass\": \"com.biz.rule.entity.OrderResult\", \"code\":0, \"content\":\"\"}\n" +
            "]\n\n" +
            "关键规则：\n" +
            "1. 场景里有几个用户就生成几个User、对应几个Order、每个Order配一个OrderResult\n" +
            "2. Order的userId要和对应User的id一致\n" +
            "3. OrderResult的code设为0，content设为空字符串，供规则修改\n" +
            "4. amount用字符串表示BigDecimal";

    /**
     * 生成测试事实JSON
     */
    public List<Map<String, Object>> generateTestFacts(String scenario) {
        LlmConfigDO config = llmService.loadConfig("zhipu_default");
        String raw = llmClient.chat(config, SYSTEM_PROMPT,
                "测试场景：" + scenario + "\n\n请生成测试事实JSON数组。");
        try {
            String json = raw.trim().replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("\\s*```$", "");
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception e) {
            log.error("LLM生成测试用例解析失败: {}", raw, e);
            throw new BizException("LLM生成测试用例失败: " + e.getMessage());
        }
    }
}
