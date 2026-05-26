package com.fag.mautc.auditor.dto;

import java.util.List;

public class OrderEventDTO {

    private String zipCode;
    private Integer customerId;
    private List<OrderItemDTO> orderItems;
    private String origin;
    private String occurredAt;

    public OrderEventDTO() {}

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public List<OrderItemDTO> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemDTO> orderItems) {
        this.orderItems = orderItems;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }

    public int totalItems() {
        if (orderItems == null) return 0;
        return orderItems.stream()
                .mapToInt(item -> item.getAmount() != null ? item.getAmount() : 0)
                .sum();
    }
}
