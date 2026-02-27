package com.fooddelivery.orderservice.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from " + getClass().getSimpleName();
    }
}