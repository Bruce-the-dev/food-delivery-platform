package com.fooddelivery.customerservice.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class customerController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from " + getClass().getSimpleName();
    }
}