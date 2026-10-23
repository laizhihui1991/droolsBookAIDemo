package com.biz.rule;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智能业务规则管理平台启动类
 * Drools 7.40.0 + Spring Boot 2.7 + 智谱AI LLM
 */
@SpringBootApplication
@MapperScan("com.biz.rule.mapper")
public class RuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuleApplication.class, args);
        System.out.println("================================================");
        System.out.println("  智能业务规则管理平台（Drools+LLM）启动成功");
        System.out.println("  访问地址: http://127.0.0.1:8080");
        System.out.println("================================================");
    }
}
