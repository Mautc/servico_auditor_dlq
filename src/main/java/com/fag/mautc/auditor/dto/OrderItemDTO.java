package com.fag.mautc.auditor.dto;

public class OrderItemDTO {

    private Integer sku;
    private Integer amount;

    public OrderItemDTO() {}

    public Integer getSku() {
        return sku;
    }

    public void setSku(Integer sku) {
        this.sku = sku;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
