package com.fooddelivery.customerservice.service;

import com.fooddelivery.customerservice.config.JwtUtil;
import com.fooddelivery.customerservice.dto.AuthRequest;
import com.fooddelivery.customerservice.dto.AuthResponse;
import com.fooddelivery.customerservice.dto.CustomerResponse;
import com.fooddelivery.customerservice.dto.RegisterRequest;
import com.fooddelivery.customerservice.model.Customer;
import com.fooddelivery.customerservice.repository.CustomerRepository;
import com.fooddelivery.customerservice.exception.DuplicateResourceException;
import com.fooddelivery.customerservice.exception.ResourceNotFoundException;
import com.fooddelivery.customerservice.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    // Temporary JWT utility placeholder; can be removed for now or replaced with a stub
    private final JwtUtil jwtUtil;


    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (customerRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken");
        }
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        Customer customer = Customer.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .deliveryAddress(request.getDeliveryAddress())
                .city(request.getCity())
                .role(Customer.Role.CUSTOMER)
                .build();

        customerRepository.save(customer);

        // For now, JWT generation can be skipped or return a placeholder token
        String token = jwtUtil.generateToken(customer.getUsername(), customer.getRole().name());
//        String token = "dummy-token"; // placeholder

        return new AuthResponse(token, customer.getId(), customer.getUsername(), customer.getRole().name());
    }

    public AuthResponse login(AuthRequest request) {
        Customer customer = customerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "username", request.getUsername()));

        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(customer.getUsername(), customer.getRole().name());
//        String token = "dummy-token"; // placeholder

        return new AuthResponse(token, customer.getId(), customer.getUsername(), customer.getRole().name());
    }

    @Transactional(readOnly = true)
    public CustomerResponse getProfile(String username) {
        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "username", username));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional
    public CustomerResponse updateProfileById(Long id, RegisterRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        if (request.getFirstName() != null) customer.setFirstName(request.getFirstName());
        if (request.getLastName() != null) customer.setLastName(request.getLastName());
        if (request.getPhone() != null) customer.setPhone(request.getPhone());
        if (request.getDeliveryAddress() != null) customer.setDeliveryAddress(request.getDeliveryAddress());
        if (request.getCity() != null) customer.setCity(request.getCity());

        return CustomerResponse.fromEntity(customerRepository.save(customer));
    }
}