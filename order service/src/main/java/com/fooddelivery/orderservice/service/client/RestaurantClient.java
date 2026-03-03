package com.fooddelivery.orderservice.service.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "restaurant-service")
public interface RestaurantClient {

    // Get restaurant info by ID
    @GetMapping("/api/restaurants/{id}")
    RestaurantResponseDTO getRestaurantById(@PathVariable("id") Long restaurantId);

//    // Optional: get all menu items of a restaurant
//    @GetMapping("/api/restaurants/{id}/menu")
//    List<MenuItemResponseDTO> getMenuByRestaurant(@PathVariable("id") Long restaurantId);

    @GetMapping("/api/restaurants/{restaurantId}/menu/{menuItemId}")
    MenuItemResponseDTO getMenuItemByRestaurant(@PathVariable Long restaurantId, @PathVariable Long menuItemId);


    @Data
     class RestaurantResponseDTO {
        private Long id;
        private String name;
        private boolean active;
        private int estimatedDeliveryMinutes;
        private String address;
    }

    @Data
     class MenuItemResponseDTO {
        private Long id;
        private String name;
        private BigDecimal price;
        private boolean available;
        private Long restaurantId;
    }
}

