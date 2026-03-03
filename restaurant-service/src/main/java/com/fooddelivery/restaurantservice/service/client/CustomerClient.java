package com.fooddelivery.restaurantservice.service.client;


import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "customer-service", path = "/api/customers")
public interface CustomerClient {
    @GetMapping("/{id}")
    CustomerResponseDto getCustomerById(@PathVariable Long id);

    @GetMapping("/by-username/{username}")
    CustomerResponseDto getByUsername(@PathVariable String username);

    @PostMapping("/{id}/promote")
    void promoteToRestaurantOwner(@PathVariable("id") Long id);

    @Data
    class CustomerResponseDto {
        private Long id;
        private String username;
        private String role;
    }
}