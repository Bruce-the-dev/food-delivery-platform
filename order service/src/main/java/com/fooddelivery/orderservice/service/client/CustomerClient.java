package com.fooddelivery.orderservice.service.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerClient {
    @GetMapping("/api/customers/{id}")
    CustomerResponseDto getCustomerById(@PathVariable Long id);

    @GetMapping("/api/customers/by-username/{username}")
    CustomerResponseDto getByUsername(@PathVariable String username);
@Data
class CustomerResponseDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String deliveryAddress;
}
}