package com.fooddelivery.deliveryservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

// DeliveryStatusUpdatedEvent.java  — goes in Delivery Service
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class DeliveryStatusUpdatedEvent implements Serializable {
        private Long deliveryId;
        private Long orderId;
        private String newStatus;
        private LocalDateTime updatedAt;
    }