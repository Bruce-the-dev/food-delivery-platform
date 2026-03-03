package com.fooddelivery.restaurantservice.controller;

import com.fooddelivery.restaurantservice.dto.*;

import com.fooddelivery.restaurantservice.model.MenuItem;
import com.fooddelivery.restaurantservice.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    // ---- Public endpoints (no auth required) ----

    @GetMapping("/search/city/{city}")
    public ResponseEntity<List<RestaurantResponse>> searchByCity(@PathVariable String city) {
        return ResponseEntity.ok(restaurantService.searchByCity(city));
    }

    @GetMapping("/search/cuisine/{type}")
    public ResponseEntity<List<RestaurantResponse>> searchByCuisine(@PathVariable String type) {
        return ResponseEntity.ok(restaurantService.searchByCuisine(type));
    }

    @GetMapping("/search/all")
    public ResponseEntity<List<RestaurantResponse>> getAllActive() {
        return ResponseEntity.ok(restaurantService.getAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getById(id));
    }


    // ---- Authenticated endpoints (restaurant owner) ----

    @PostMapping
    public ResponseEntity<RestaurantResponse> create(
//            Authentication auth,
            @RequestParam Long ownerId,
            @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.createRestaurant(ownerId, request));
    }

    @PostMapping("/{restaurantId}/menu")
    public ResponseEntity<MenuItemResponse> addMenuItem(
            @PathVariable Long restaurantId,
            @RequestParam String OwnerUsername ,
            @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.addMenuItem(restaurantId,OwnerUsername, request));
    }

    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<List<MenuItem>> getMenu(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(restaurantService.getMenuItems(restaurantId));
    }

    @PutMapping("/menu/{itemId}")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable Long itemId,
            @RequestParam String  OwnerUsername ,
            @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(restaurantService.updateMenuItem(itemId, OwnerUsername,request));
    }

    @PatchMapping("/menu/{itemId}/toggle")
    public ResponseEntity<Void> toggleAvailability(@PathVariable Long itemId) {
        restaurantService.toggleMenuItemAvailability(itemId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{restaurantId}/menu/{menuItemId}")
    public ResponseEntity<MenuItemResponse> getMenuItemByRestaurant(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId) {

        return ResponseEntity.ok(
                restaurantService.getMenuItemByRestaurant(restaurantId, menuItemId)
        );
    }
}
