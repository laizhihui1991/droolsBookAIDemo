package com.biz.rule.entity;

import java.io.Serializable;

/**
 * 预置实体：规则执行结果
 */
public class OrderResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String content;

    public OrderResult() {}

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @Override
    public String toString() {
        return "OrderResult{code=" + code + ", content='" + content + "'}";
    }
}
