package com.fooddelivery.orderservice.event;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
@Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public  class DeliveryStatusUpdatedEvent implements Serializable {
        private Long deliveryId;
        private Long orderId;
        private String newStatus;
        private LocalDateTime updatedAt;
    }