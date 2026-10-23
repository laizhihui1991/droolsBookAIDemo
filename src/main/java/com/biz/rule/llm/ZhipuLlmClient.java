package com.biz.rule.llm;

import com.biz.rule.common.exception.BizException;
import com.biz.rule.entity.LlmConfigDO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智谱AI LLM 客户端
 * 兼容智谱 v4 /chat/completions 接口格式（OpenAI 风格）
 */
@Slf4j
@Component
public class ZhipuLlmClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 同步调用，返回 assistant 文本
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     */
    public String chat(LlmConfigDO config, String systemPrompt, String userPrompt) {
        if (config == null) {
            throw new BizException("LLM配置为空");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModelName());
            body.put("temperature", 0.1);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(roleMsg("system", systemPrompt));
            messages.add(roleMsg("user", userPrompt));
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    config.getApiUrl(), req, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new BizException("LLM调用失败: HTTP " + resp.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                throw new BizException("LLM返回异常: " + resp.getBody());
            }
            return choices.get(0).path("message").path("content").asText();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用智谱AI异常", e);
            throw new BizException("调用智谱AI异常: " + e.getMessage());
        }
    }

    private Map<String, String> roleMsg(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }
}
