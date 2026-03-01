package com.fooddelivery.restaurantservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private String category;

    private boolean available;

    private String imageUrl;

    // Still same-domain, but we can store restaurantId instead of entity
    @Column(nullable = false)
    private Long restaurantId;

    @PrePersist
    protected void onCreate() {
        available = true;
    }
}