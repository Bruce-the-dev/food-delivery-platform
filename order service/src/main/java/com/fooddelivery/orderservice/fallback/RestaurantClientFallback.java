package com.fooddelivery.orderservice.fallback;

import com.fooddelivery.orderservice.client.RestaurantClient;
import com.fooddelivery.orderservice.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class RestaurantClientFallback implements RestaurantClient {

    @Override
    public RestaurantResponseDTO getRestaurantById(Long id) {
        throw new ServiceUnavailableException("Restaurant Service is currently unavailable. Please try again later.");
    }

    @Override
    public MenuItemResponseDTO getMenuItemByRestaurant(Long restaurantId, Long menuItemId) {
        throw new ServiceUnavailableException("Restaurant Service is currently unavailable. Please try again later.");
    }
}