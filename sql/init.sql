-- =============================================================
-- 智能业务规则管理平台（Drools+LLM）数据库初始化脚本
-- DB: drools_ai_platform
-- 适配 MySQL 5.7+ / 8.0
-- =============================================================

CREATE DATABASE IF NOT EXISTS `drools_ai_platform`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `drools_ai_platform`;

-- -------------------------------------------------------------
-- 1. 业务实体表 rule_entity
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `rule_entity`;
CREATE TABLE `rule_entity` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `entity_code`  VARCHAR(64)  NOT NULL COMMENT '实体唯一编码',
    `entity_name`  VARCHAR(128) NOT NULL COMMENT '实体名称',
    `package_name` VARCHAR(256) NOT NULL DEFAULT 'com.biz.rule.entity' COMMENT '包名',
    `field_json`    TEXT         NOT NULL COMMENT '字段定义JSON',
    `import_list`   TEXT         COMMENT '额外导入包列表，逗号分隔',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0草稿 1启用 2停用',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`       VARCHAR(512) COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_entity_code` (`entity_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务实体表';

-- -------------------------------------------------------------
-- 2. 规则主表 rule_info
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `rule_info`;
CREATE TABLE `rule_info` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_code`      VARCHAR(64)  NOT NULL COMMENT '规则唯一编码',
    `rule_name`      VARCHAR(128) NOT NULL COMMENT '规则名称',
    `rule_type`       VARCHAR(32)  NOT NULL COMMENT '规则分类：营销/风控/审批',
    `package_name`   VARCHAR(256) NOT NULL COMMENT '规则包名',
    `current_version` INT         NOT NULL DEFAULT 0 COMMENT '当前生效版本号',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0草稿 1审核中 2已发布 3已停用',
    `create_user`    VARCHAR(64)  COMMENT '创建人',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_code` (`rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则主表';

-- -------------------------------------------------------------
-- 3. 规则版本表 rule_version
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `rule_version`;
CREATE TABLE `rule_version` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_id`              BIGINT       NOT NULL COMMENT '关联rule_info.id',
    `version`              INT          NOT NULL COMMENT '版本号',
    `rule_json`            LONGTEXT     NOT NULL COMMENT '标准规则JSON模型',
    `rule_drl`             LONGTEXT     COMMENT '生成的完整DRL代码',
    `entity_refs`          VARCHAR(512) COMMENT '关联实体编码列表，逗号分隔',
    `conflict_check_result` TEXT        COMMENT '冲突检测结果JSON',
    `syntax_check_result`  TEXT        COMMENT '语法校验结果JSON',
    `create_user`          VARCHAR(64) COMMENT '创建人',
    `create_time`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `remark`               VARCHAR(512) COMMENT '版本备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_version` (`rule_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则版本表';

-- -------------------------------------------------------------
-- 4. 测试用例表 rule_test_case
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `rule_test_case`;
CREATE TABLE `rule_test_case` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_id`       BIGINT       NOT NULL COMMENT '关联规则ID',
    `rule_version`  INT          NOT NULL COMMENT '关联规则版本',
    `case_name`     VARCHAR(128) NOT NULL COMMENT '用例名称',
    `fact_json`     LONGTEXT     NOT NULL COMMENT '测试事实数据JSON',
    `expect_result` LONGTEXT     COMMENT '预期执行结果JSON',
    `actual_result`  LONGTEXT     COMMENT '实际执行结果JSON',
    `pass_status`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否通过：0未执行 1通过 2不通过',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `execute_time`  DATETIME     COMMENT '最近执行时间',
    PRIMARY KEY (`id`),
    KEY `idx_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例表';

-- -------------------------------------------------------------
-- 5. 审核流程表 rule_audit
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `rule_audit`;
CREATE TABLE `rule_audit` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_id`      BIGINT       NOT NULL COMMENT '关联规则ID',
    `rule_version` INT          NOT NULL COMMENT '关联版本',
    `submit_user`  VARCHAR(64)  NOT NULL COMMENT '提交人',
    `audit_user`   VARCHAR(64)  COMMENT '审核人',
    `audit_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态：0待审核 1通过 2驳回',
    `audit_opinion` VARCHAR(512) COMMENT '审核意见',
    `submit_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `audit_time`   DATETIME     COMMENT '审核时间',
    PRIMARY KEY (`id`),
    KEY `idx_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核流程表';

-- -------------------------------------------------------------
-- 6. LLM配置表 llm_config
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `llm_config`;
CREATE TABLE `llm_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_code`  VARCHAR(32)  NOT NULL COMMENT '配置编码',
    `provider`     VARCHAR(32)  NOT NULL DEFAULT 'zhipu' COMMENT '厂商：zhipu/其他',
    `model_name`   VARCHAR(64)  NOT NULL DEFAULT 'glm-4' COMMENT '模型名称',
    `api_key`      VARCHAR(256) NOT NULL COMMENT 'API密钥（加密存储）',
    `api_url`      VARCHAR(256) NOT NULL COMMENT '接口地址',
    `timeout`      INT          NOT NULL DEFAULT 60 COMMENT '超时时间（秒）',
    `enable`       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_code` (`config_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM配置表';

-- -------------------------------------------------------------
-- 初始化默认智谱AI配置（请替换 api_key 为真实值）
-- -------------------------------------------------------------
INSERT INTO `llm_config` (`config_code`, `provider`, `model_name`, `api_key`, `api_url`, `timeout`, `enable`)
VALUES ('zhipu_default', 'zhipu', 'glm-4', 'YOUR_ZHIPU_API_KEY',
        'https://open.bigmodel.cn/api/paas/v4/chat/completions', 60, 1);
