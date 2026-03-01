package com.fooddelivery.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {
    @NotNull private Long menuItemId;
    @Positive private int quantity;
    private String specialInstructions;
    private String menuItemName;
    private BigDecimal unitPrice;
}
