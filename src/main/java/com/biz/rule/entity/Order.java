package com.biz.rule.entity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预置实体：订单
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    /** 原始金额 */
    private BigDecimal amount;
    /** 折后金额 */
    private BigDecimal finalAmount;
    /** 订单状态：NEW / PAID / CANCELLED */
    private String status;

    public Order() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", userId=" + userId + ", amount=" + amount + "}";
    }
}
