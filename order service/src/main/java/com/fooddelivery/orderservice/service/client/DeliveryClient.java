package com.fooddelivery.orderservice.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "delivery-service")
public interface DeliveryClient {

    @PostMapping("/api/deliveries/assign/{orderId}")
    void assignDelivery(@PathVariable("orderId") Long orderId);
}