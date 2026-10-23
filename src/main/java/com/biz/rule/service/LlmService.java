package com.biz.rule.service;

import com.biz.rule.common.exception.BizException;
import com.biz.rule.engine.model.EntityModel;
import com.biz.rule.entity.LlmConfigDO;
import com.biz.rule.mapper.LlmConfigMapper;
import com.biz.rule.llm.PromptBuilder;
import com.biz.rule.llm.ZhipuLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LLM 智能规则生成 Service
 */
@Slf4j
@Service
public class LlmService {

    @Autowired
    private ZhipuLlmClient llmClient;

    @Autowired
    private PromptBuilder promptBuilder;

    @Autowired
    private LlmConfigMapper llmConfigMapper;

    /** data.sql / init.sql 中的密钥占位符 */
    private static final String API_KEY_PLACEHOLDER = "YOUR_ZHIPU_API_KEY";

    @Value("${llm.api-key:}")
    private String configApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据业务描述生成规则JSON草案
     *
     * @param packageName   规则包名
     * @param entities      可用实体列表（已解析为EntityModel）
     * @param businessDesc  业务自然语言描述
     * @param configCode    LLM配置编码，默认 zhipu_default
     */
    public String generateRuleJson(String packageName,
                                   List<EntityModel> entities,
                                   String businessDesc,
                                   String configCode) {
        if (businessDesc == null || businessDesc.trim().isEmpty()) {
            throw new BizException("业务描述不能为空");
        }

        LlmConfigDO config = loadConfig(configCode == null ? "zhipu_default" : configCode);
        String userPrompt = promptBuilder.buildUserPrompt(businessDesc);

        log.info("调用LLM生成规则, businessDesc={}", businessDesc);
        String raw = llmClient.chat(config, promptBuilder.getSystemPrompt(), userPrompt);

        // 清理可能的 markdown 包裹
        return cleanJson(raw);
    }

    public LlmConfigDO loadConfig(String code) {
        LlmConfigDO config = llmConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LlmConfigDO>()
                        .eq(LlmConfigDO::getConfigCode, code)
                        .eq(LlmConfigDO::getEnable, 1));
        if (config == null) {
            throw new BizException("未找到启用的LLM配置: " + code);
        }
        // 数据库中未配置真实密钥时，回退到 application.yml 的 llm.api-key（环境变量 LLM_API_KEY）
        String dbKey = config.getApiKey();
        if (dbKey == null || dbKey.trim().isEmpty() || API_KEY_PLACEHOLDER.equals(dbKey.trim())) {
            if (configApiKey == null || configApiKey.trim().isEmpty()) {
                throw new BizException("LLM API Key 未配置，请设置环境变量 LLM_API_KEY 或更新 llm_config 表");
            }
            config.setApiKey(configApiKey.trim());
        }
        return config;
    }

    /**
     * 去除 LLM 可能包裹的 ```json ... ``` 标记
     */
    private String cleanJson(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) {
                s = s.substring(firstNl + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
        }
        return s.trim();
    }
}
