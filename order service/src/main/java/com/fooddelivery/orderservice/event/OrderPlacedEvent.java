package com.fooddelivery.orderservice.event;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPlacedEvent implements Serializable {

    private Long orderId;
    private Long customerId;
    private String customerName;
    private Long restaurantId;
    private String restaurantName;
    private List<OrderItemEvent> items;
    private BigDecimal totalAmount;
    private String deliveryAddress;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemEvent implements Serializable {
        private Long menuItemId;
        private String itemName;
        private int quantity;
        private BigDecimal unitPrice;

    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCancelledEvent implements Serializable {
        private Long orderId;
        private Long customerId;
        private String reason;
    }
    // DeliveryStatusUpdatedEvent.java  — goes in Delivery Service

}
