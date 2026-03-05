package com.fooddelivery.orderservice.listener;

import com.fooddelivery.orderservice.config.RabbitConfig;
import com.fooddelivery.orderservice.event.DeliveryStatusUpdatedEvent;
import com.fooddelivery.orderservice.event.OrderPlacedEvent;
import com.fooddelivery.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// OrderEventListener.java in Order Service
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitConfig.DELIVERY_STATUS_QUEUE)
    public void handleDeliveryStatusUpdated(DeliveryStatusUpdatedEvent event) {
        log.info("Received DeliveryStatusUpdatedEvent: order={} status={}",
                event.getOrderId(), event.getNewStatus());
        String orderStatus = mapToOrderStatus(event.getNewStatus());
        if(orderStatus != null) {

            orderService.updateStatus(event.getOrderId(), orderStatus);
        }else {
            log.info("No order status mapping for delivery status: {}, skipping", event.getNewStatus());
        }
    }
    private String mapToOrderStatus(String deliveryStatus) {
        return switch (deliveryStatus) {
            case "ASSIGNED"   -> "CONFIRMED";
            case "PICKED_UP", "IN_TRANSIT" -> "OUT_FOR_DELIVERY";
            case "DELIVERED"  -> "DELIVERED";
            case "FAILED"     -> "CANCELLED";
            default           -> null;
        };
    }
}