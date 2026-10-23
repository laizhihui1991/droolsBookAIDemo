-- =============================================================
-- 智能业务规则管理平台 - 精简版表结构（3张表）
-- 实体为Java类（User/Order/OrderResult），不需要实体表
-- 测试为临时执行，不需要测试用例表
-- 发布直接入库，不需要审核流程表
-- =============================================================

-- 1. 规则主表
DROP TABLE IF EXISTS `rule_info`;
CREATE TABLE `rule_info` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_code`      VARCHAR(64)  NOT NULL COMMENT '规则唯一编码',
    `rule_name`      VARCHAR(128) NOT NULL COMMENT '规则名称',
    `rule_type`      VARCHAR(32)  NOT NULL DEFAULT 'marketing' COMMENT '规则分类',
    `package_name`   VARCHAR(256) NOT NULL COMMENT '规则包名',
    `current_version` INT         NOT NULL DEFAULT 0 COMMENT '当前生效版本号',
    `status`         TINYINT      NOT NULL DEFAULT 2 COMMENT '状态：2已发布 3已停用',
    `create_user`    VARCHAR(64)  DEFAULT 'llm' COMMENT '创建人',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_code` (`rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则主表';

-- 2. 规则版本表（存JSON和DRL）
DROP TABLE IF EXISTS `rule_version`;
CREATE TABLE `rule_version` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_id`     BIGINT       NOT NULL COMMENT '关联rule_info.id',
    `version`     INT          NOT NULL COMMENT '版本号',
    `rule_json`   LONGTEXT     NOT NULL COMMENT '标准规则JSON',
    `rule_drl`    LONGTEXT     COMMENT '生成的DRL代码',
    `create_user` VARCHAR(64)  DEFAULT 'llm' COMMENT '创建人',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `remark`      VARCHAR(512) COMMENT '版本备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_version` (`rule_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则版本表';

-- 3. LLM配置表
DROP TABLE IF EXISTS `llm_config`;
CREATE TABLE `llm_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_code`  VARCHAR(32)  NOT NULL COMMENT '配置编码',
    `provider`     VARCHAR(32)  NOT NULL DEFAULT 'zhipu' COMMENT '厂商',
    `model_name`   VARCHAR(64)  NOT NULL DEFAULT 'glm-4-flash' COMMENT '模型名称',
    `api_key`      VARCHAR(256) NOT NULL COMMENT 'API密钥',
    `api_url`      VARCHAR(256) NOT NULL COMMENT '接口地址',
    `timeout`      INT          NOT NULL DEFAULT 60 COMMENT '超时秒数',
    `enable`       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_code` (`config_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM配置表';
