package com.fooddelivery.deliveryservice.dto;

import com.fooddelivery.deliveryservice.model.Delivery;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliveryResponse {
    private Long id;
    private String status;
    private String driverName;
    private String driverPhone;
    private String pickupAddress;
    private String deliveryAddress;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;

    // Microservice-friendly: only store orderId
    private Long orderId;

    public static DeliveryResponse fromEntity(Delivery d) {
        DeliveryResponse dto = new DeliveryResponse();
        dto.setId(d.getId());
        dto.setStatus(d.getStatus().name());
        dto.setDriverName(d.getDriverName());
        dto.setDriverPhone(d.getDriverPhone());
        dto.setPickupAddress(d.getPickupAddress());
        dto.setDeliveryAddress(d.getDeliveryAddress());
        dto.setAssignedAt(d.getAssignedAt());
        dto.setPickedUpAt(d.getPickedUpAt());
        dto.setDeliveredAt(d.getDeliveredAt());
        dto.setCreatedAt(d.getCreatedAt());

        dto.setOrderId(d.getOrderId());  // only store orderId, no entity traversal
        return dto;
    }
}