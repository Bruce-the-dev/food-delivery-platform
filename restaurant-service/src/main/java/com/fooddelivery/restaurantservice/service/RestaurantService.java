package com.fooddelivery.restaurantservice.service;

import com.fooddelivery.restaurantservice.dto.*;

import com.fooddelivery.restaurantservice.exception.*;
import com.fooddelivery.restaurantservice.model.*;
import com.fooddelivery.restaurantservice.repository.*;
import com.fooddelivery.restaurantservice.service.client.CustomerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final CustomerClient customerClient;

    @Transactional
    public RestaurantResponse createRestaurant(Long ownerId, RestaurantRequest request) {

        CustomerClient.CustomerResponseDto owner = customerClient.getCustomerById(ownerId);
        if (owner == null) {
            throw new ResourceNotFoundException("Customer", "id", ownerId);
        }
        // 2. Promote to RESTAURANT_OWNER if needed
        if ("CUSTOMER".equals(owner.getRole())) {
            customerClient.promoteToRestaurantOwner(ownerId);
        }

        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .description(request.getDescription())
                .cuisineType(request.getCuisineType())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .estimatedDeliveryMinutes(request.getEstimatedDeliveryMinutes())
                .ownerId(ownerId)
                .build();

        return RestaurantResponse.fromEntity(restaurantRepository.save(restaurant));
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
        return RestaurantResponse.fromEntity(restaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> searchByCity(String city) {
        return restaurantRepository.findByCityIgnoreCaseAndActiveTrue(city)
                .stream().map(RestaurantResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> searchByCuisine(String cuisineType) {
        return restaurantRepository.findByCuisineTypeIgnoreCaseAndActiveTrue(cuisineType)
                .stream().map(RestaurantResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAllActive() {
        return restaurantRepository.findByActiveTrue()
                .stream().map(RestaurantResponse::fromEntity).toList();
    }


    @Transactional
    public MenuItemResponse addMenuItem(Long restaurantId,String ownerUsername, MenuItemRequest request) {
        // Only check restaurant exists
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

        CustomerClient.CustomerResponseDto owner = customerClient.getByUsername(ownerUsername);
        if (owner == null) {
            throw new ResourceNotFoundException("Customer", "Username", ownerUsername);
        }
        if (!owner.getId().equals(restaurant.getOwnerId())) {
            throw new UnauthorizedException("You don't own this restaurant");
        }

        MenuItem item = MenuItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .restaurant(restaurant)  // ID only
                .available(true)
                .build();

        return MenuItemResponse.fromEntity( menuItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<MenuItem> getMenuItems(Long restaurantId) {
        return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId);
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long itemId, String ownerUsername,MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemId));

        // Fetch the restaurant
        Restaurant restaurant = restaurantRepository.findById(item.getRestaurant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", item.getRestaurant().getId()));

        // Validate ownership via Customer service
        CustomerClient.CustomerResponseDto owner = customerClient.getByUsername(ownerUsername);
        if (owner == null) {
            throw new ResourceNotFoundException("Customer", "username", ownerUsername);
        }

        if (!owner.getId().equals(restaurant.getOwnerId())) {
            throw new UnauthorizedException("You don't own this restaurant");
        }
        if (request.getName() != null) item.setName(request.getName());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        if (request.getCategory() != null) item.setCategory(request.getCategory());
        if (request.getImageUrl() != null) item.setImageUrl(request.getImageUrl());

        return MenuItemResponse.fromEntity( menuItemRepository.save(item));
    }

    @Transactional
    public void toggleMenuItemAvailability(Long itemId) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemId));
        item.setAvailable(!item.isAvailable());
        menuItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemByRestaurant(Long restaurantId, Long menuItemId) {

        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("MenuItem", "id", menuItemId));

        if (!menuItem.getRestaurant().getId().equals(restaurantId)) {
            throw new IllegalStateException(
                    "Menu item does not belong to this restaurant");
        }

        return MenuItemResponse.fromEntity(menuItem);
    }
}
