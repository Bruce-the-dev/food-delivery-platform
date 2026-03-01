package com.fooddelivery.restaurantservice.repository;


import com.fooddelivery.restaurantservice.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByActiveTrue();
    List<Restaurant> findByCityIgnoreCaseAndActiveTrue(String city);
    List<Restaurant> findByCuisineTypeIgnoreCaseAndActiveTrue(String cuisineType);
    List<Restaurant> findByOwnerId(Long ownerId);
}
