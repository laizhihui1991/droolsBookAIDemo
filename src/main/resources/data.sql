-- =============================================================
-- 初始化默认 LLM 配置：智谱 GLM-4-Flash（永久免费）
-- api_key 使用占位符，运行时由环境变量 LLM_API_KEY（application.yml 中 llm.api-key）替换
-- =============================================================

INSERT INTO `llm_config` (`config_code`, `provider`, `model_name`, `api_key`, `api_url`, `timeout`, `enable`)
VALUES ('zhipu_default', 'zhipu', 'glm-4-flash',
        'YOUR_ZHIPU_API_KEY',
        'https://open.bigmodel.cn/api/paas/v4/chat/completions',
        60, 1);
