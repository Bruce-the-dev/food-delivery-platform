package com.fooddelivery.customerservice.controller;

import com.fooddelivery.customerservice.dto.AuthRequest;
import com.fooddelivery.customerservice.dto.AuthResponse;
import com.fooddelivery.customerservice.dto.CustomerResponse;
import com.fooddelivery.customerservice.dto.RegisterRequest;
import com.fooddelivery.customerservice.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(customerService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(customerService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getMyProfile() {
        // Temporary: always return customer with ID 1
        return ResponseEntity.ok(customerService.getById(1L));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @PutMapping("/me")
    public ResponseEntity<CustomerResponse> updateProfile(@RequestBody RegisterRequest request) {
        // Temporary: always update customer with ID 1
        return ResponseEntity.ok(customerService.updateProfileById(1L, request));
    }
}