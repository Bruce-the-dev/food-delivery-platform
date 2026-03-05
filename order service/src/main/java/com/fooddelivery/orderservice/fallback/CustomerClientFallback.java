package com.fooddelivery.orderservice.fallback;

import com.fooddelivery.orderservice.client.CustomerClient;
import com.fooddelivery.orderservice.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class CustomerClientFallback implements CustomerClient {

    @Override
    public CustomerResponseDto getCustomerById(Long id) {
        throw new ServiceUnavailableException("Customer Service is currently unavailable. Please try again later.");
    }

    @Override
    public CustomerResponseDto getByUsername(String username) {
        throw new ServiceUnavailableException("Customer Service is currently unavailable. Please try again later.");
    }
}