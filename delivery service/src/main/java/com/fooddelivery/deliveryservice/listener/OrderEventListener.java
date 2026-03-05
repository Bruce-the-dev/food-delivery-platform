package com.fooddelivery.deliveryservice.listener;

import com.fooddelivery.deliveryservice.config.RabbitConfig;
import com.fooddelivery.deliveryservice.event.OrderPlacedEvent;
import com.fooddelivery.deliveryservice.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final DeliveryService deliveryService;

    @RabbitListener(queues = RabbitConfig.ORDER_PLACED_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        // When a new order is placed, assign a delivery
        log.info("Received OrderPlacedEvent for order: {}", event.getOrderId());
        deliveryService.assignDelivery(event);
    }
    @RabbitListener(queues = RabbitConfig.ORDER_PLACED_QUEUE)  // ← add this
    public void handleOrderCancelled(OrderPlacedEvent.OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for order: {}", event.getOrderId());
        deliveryService.cancelDelivery(event.getOrderId());
    }
}