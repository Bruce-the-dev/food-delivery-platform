package com.fooddelivery.deliveryservice.service;

import com.fooddelivery.deliveryservice.config.RabbitConfig;
import com.fooddelivery.deliveryservice.dto.DeliveryResponse;
import com.fooddelivery.deliveryservice.event.DeliveryStatusUpdatedEvent;
import com.fooddelivery.deliveryservice.exception.ResourceNotFoundException;
import com.fooddelivery.deliveryservice.event.OrderPlacedEvent;
import com.fooddelivery.deliveryservice.model.Delivery;
import com.fooddelivery.deliveryservice.model.Delivery.DeliveryStatus;
import com.fooddelivery.deliveryservice.repository.DeliveryRepository;
//import com.fooddelivery.deliveryservice.service.client.OrderClient;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final DeliveryRepository deliveryRepository;
//    private final OrderClient orderClient;
    private final AmqpTemplate amqpTemplate;

    // Simulated driver pool
    private static final String[] DRIVERS = {
            "Carlos Martinez", "Sarah Johnson", "Mike Chen", "Priya Patel", "James Wilson"
    };
    private static final String[] PHONES = {
            "+1-555-0101", "+1-555-0102", "+1-555-0103", "+1-555-0104", "+1-555-0105"
    };


    /**
     * Creates a delivery for a given orderId asynchronously.
     * Microservices only store orderId, pickupAddress, deliveryAddress.
     */
    @Transactional
    public DeliveryResponse assignDelivery(OrderPlacedEvent event) {
        int driverIndex = (int) (Math.random() * DRIVERS.length);

        Delivery delivery = Delivery.builder()
                .orderId(event.getOrderId())                 // only store orderId
                .status(DeliveryStatus.ASSIGNED)
                .driverName(DRIVERS[driverIndex])
                .driverPhone(PHONES[driverIndex])
                .pickupAddress(event.getRestaurantName()) // in microservice, could come from event
                .deliveryAddress(event.getDeliveryAddress()) // in microservice, could come from event
                .assignedAt(LocalDateTime.now())
                .build();

        deliveryRepository.save(delivery);

        log.info("Delivery assigned to {} for order #{} — Customer: {}, Restaurant: {}",
                DRIVERS[driverIndex],
                event.getOrderId(),
                event.getCustomerName(),
                event.getRestaurantName());
// TODO: Publish DeliveryAssignedEvent via RabbitMQ
        return DeliveryResponse.fromEntity(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getById(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", deliveryId));
        return DeliveryResponse.fromEntity(delivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getByOrderId(Long orderId) {
        List<Delivery> delivery = deliveryRepository.findByOrderId(orderId);
        if (delivery.isEmpty()){
                throw new ResourceNotFoundException("Delivery", "orderId", orderId);}
        return delivery.stream().map(DeliveryResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getByStatus(String status) {
        DeliveryStatus deliveryStatus = DeliveryStatus.valueOf(status.toUpperCase());
        return deliveryRepository.findByStatus(deliveryStatus)
                .stream()
                .map(DeliveryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public DeliveryResponse updateStatus(Long deliveryId, String status) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", deliveryId));

        DeliveryStatus newStatus = DeliveryStatus.valueOf(status.toUpperCase());
        delivery.setStatus(newStatus);

        if (newStatus == DeliveryStatus.PICKED_UP) {
            delivery.setPickedUpAt(LocalDateTime.now());
        } else if (newStatus == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(LocalDateTime.now());
        }
        Delivery saved = deliveryRepository.save(delivery);
        // Publish event instead of calling Order Service directly
        DeliveryStatusUpdatedEvent event = DeliveryStatusUpdatedEvent.builder()
                .deliveryId(saved.getId())
                .orderId(saved.getOrderId())
                .newStatus(newStatus.name())
                .updatedAt(LocalDateTime.now())
                .build();
        amqpTemplate.convertAndSend(
                RabbitConfig.DELIVERY_EXCHANGE,
                RabbitConfig.DELIVERY_STATUS_KEY,
                event
        );
            log.info("DeliveryStatusUpdatedEvent published: order={} status={}", saved.getOrderId(), newStatus);

        return DeliveryResponse.fromEntity(deliveryRepository.save(delivery));
    }

    @Transactional
    public void cancelDelivery(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", deliveryId));
        delivery.setStatus(DeliveryStatus.FAILED);
        deliveryRepository.save(delivery);

        log.info("Delivery #{} cancelled", deliveryId);
    }

}