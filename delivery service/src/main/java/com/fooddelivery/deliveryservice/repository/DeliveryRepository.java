package com.fooddelivery.deliveryservice.repository;

import com.fooddelivery.deliveryservice.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByOrderId(Long orderId);
    List<Delivery> findByStatus(Delivery.DeliveryStatus status);
    List<Delivery> findByDriverNameIgnoreCase(String driverName);
}