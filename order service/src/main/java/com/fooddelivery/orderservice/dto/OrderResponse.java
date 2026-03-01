package com.fooddelivery.orderservice.dto;

import com.fooddelivery.orderservice.model.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;
    private String deliveryAddress;
    private String specialInstructions;
    private LocalDateTime createdAt;
    private LocalDateTime estimatedDeliveryTime;

    // ---- Microservices approach: only IDs and snapshot data ----
    private Long customerId;
    private String customerName;   // optional snapshot, stored at order creation
    private Long restaurantId;
    private String restaurantName; // optional snapshot

    // Delivery info snapshot
    private String deliveryStatus; // updated via Delivery Service events
    private String driverName;     // updated via Delivery Service events
    private String driverPhone;    // updated via Delivery Service events

    private List<OrderItemDetail> items;

    @Data
    public static class OrderItemDetail {
        private Long menuItemId;       // ID only
        private String itemName;       // snapshot at order creation
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private String specialInstructions;
    }

    // Factory method using only Order entity fields and snapshots
    public static OrderResponse fromEntity(Order o) {
        OrderResponse dto = new OrderResponse();
        dto.setId(o.getId());
        dto.setStatus(o.getStatus().name());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setDeliveryFee(o.getDeliveryFee());
        dto.setDeliveryAddress(o.getDeliveryAddress());
        dto.setSpecialInstructions(o.getSpecialInstructions());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setEstimatedDeliveryTime(o.getEstimatedDeliveryTime());

        // Use snapshot data instead of cross-domain entities
        dto.setCustomerId(o.getCustomerId());
        dto.setCustomerName(o.getCustomerName());
        dto.setRestaurantId(o.getRestaurantId());
        dto.setRestaurantName(o.getRestaurantName());

        if (o.getDeliveryStatus() != null) {
            dto.setDeliveryStatus(o.getDeliveryStatus());
            dto.setDriverName(o.getDriverName());
            dto.setDriverPhone(o.getDriverPhone());
        }

        dto.setItems(o.getItems().stream().map(item -> {
            OrderItemDetail detail = new OrderItemDetail();
            detail.setMenuItemId(item.getMenuItemId());
            detail.setItemName(item.getItemName());
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(item.getUnitPrice());
            detail.setSubtotal(item.getSubtotal());
            detail.setSpecialInstructions(item.getSpecialInstructions());
            return detail;
        }).toList());

        return dto;
    }
}