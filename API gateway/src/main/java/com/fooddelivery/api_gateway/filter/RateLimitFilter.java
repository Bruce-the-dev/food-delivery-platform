package com.fooddelivery.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    // Max requests per window
    private static final int MAX_REQUESTS = 5;
    // Time window in milliseconds (1 minute)
    private static final long WINDOW_MS = 60_000;

    // Only rate limit these paths
    private static final String RATE_LIMITED_PATH = "/api/orders";

    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only rate limit POST /api/orders
        if (path.startsWith(RATE_LIMITED_PATH) && method.equals("POST")) {
            String clientIp = getClientIp(request);
            RequestCounter counter = requestCounts.computeIfAbsent(clientIp, k -> new RequestCounter());

            if (!counter.allowRequest()) {
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\": \"Too many requests. Maximum " + MAX_REQUESTS + " order requests per minute allowed.\"}"
                );
                return;
            }

            log.info("Rate limit check passed for IP: {} — {}/{} requests used",
                    clientIp, counter.getCount(), MAX_REQUESTS);
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Token bucket counter per IP
    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        public boolean allowRequest() {
            long now = System.currentTimeMillis();
            long windowStartTime = windowStart.get();

            // Reset window if expired
            if (now - windowStartTime > WINDOW_MS) {
                windowStart.set(now);
                count.set(0);
            }

            return count.incrementAndGet() <= MAX_REQUESTS;
        }

        public int getCount() {
            return count.get();
        }
    }
}