# Architecture Documentation

## Table of Contents

- [System Overview](#system-overview)
- [Service Architecture](#service-architecture)
- [Database Architecture](#database-architecture)
- [API Gateway](#api-gateway)
- [Service Discovery](#service-discovery)
- [Event-Driven Messaging](#event-driven-messaging)
- [Fault Tolerance](#fault-tolerance)
- [Inter-Service Communication](#inter-service-communication)
- [Security Architecture](#security-architecture)
- [Docker Architecture](#docker-architecture)

---

## System Overview

The Food Delivery Platform is a microservices system migrated from a monolithic Spring Boot application. The system decomposes the monolith into 4 domain services, each independently deployable with its own database.

```
                        ┌─────────────────────────────────────────────┐
                        │              CLIENT (Postman / Browser)      │
                        └───────────────────────┬─────────────────────┘
                                                 │ HTTP
                                                 ▼
                        ┌─────────────────────────────────────────────┐
                        │                  API GATEWAY                 │
                        │                  port: 8080                  │
                        │                                              │
                        │  ┌─────────────────┐  ┌──────────────────┐  │
                        │  │  JWT Auth Filter │  │ Rate Limit Filter│  │
                        │  └─────────────────┘  └──────────────────┘  │
                        │                                              │
                        │  Routes:                                     │
                        │  /api/customers/**  → customer-service       │
                        │  /api/restaurants/** → restaurant-service    │
                        │  /api/orders/**     → order-service          │
                        │  /api/deliveries/** → delivery-service       │
                        └──────┬──────────┬──────────┬──────────┬─────┘
                               │          │          │          │
                    lb://       │          │          │          │  lb://
              ┌────────────────▼─┐  ┌─────▼──────┐  │  ┌───────▼──────┐
              │  CUSTOMER SERVICE│  │ RESTAURANT  │  │  │   DELIVERY   │
              │   port: 8081     │  │  SERVICE    │  │  │   SERVICE    │
              │                  │  │  port: 8082 │  │  │  port: 8084  │
              │  - register      │  │             │  │  │              │
              │  - login         │  │  - create   │  │  │  - track     │
              │  - JWT issuance  │  │  - menu mgmt│  │  │  - update    │
              └────────┬─────────┘  └──────┬──────┘  │  └──────┬───────┘
                       │                   │          │         │
              ┌────────▼─────────┐  ┌──────▼──────┐  │  ┌──────▼───────┐
              │   customer_db    │  │ restaurant_db│  │  │  delivery_db │
              │   PostgreSQL     │  │  PostgreSQL  │  │  │  PostgreSQL  │
              └──────────────────┘  └─────────────┘  │  └──────────────┘
                                                      │
                                          ┌───────────▼──────────┐
                                          │     ORDER SERVICE     │
                                          │      port: 8083       │
                                          │                       │
                                          │  - place order        │
                                          │  - track status       │
                                          │  - feign: customer    │
                                          │  - feign: restaurant  │
                                          └───────────┬───────────┘
                                                      │
                                          ┌───────────▼───────────┐
                                          │       order_db        │
                                          │      PostgreSQL       │
                                          └───────────────────────┘

                    ┌──────────────────────────────────────────────┐
                    │                  RABBITMQ                     │
                    │               port: 5672                      │
                    │                                               │
                    │  order.exchange                               │
                    │    order.placed    ──▶ delivery-service       │
                    │    order.cancelled ──▶ delivery-service       │
                    │                                               │
                    │  delivery.exchange                            │
                    │    delivery.status.updated ──▶ order-service  │
                    └──────────────────────────────────────────────┘

                    ┌──────────────────────────────────────────────┐
                    │               EUREKA SERVER                   │
                    │               port: 8761                      │
                    │                                               │
                    │  Registered Services:                         │
                    │  - API-GATEWAY                                │
                    │  - CUSTOMER-SERVICE                           │
                    │  - RESTAURANT-SERVICE                         │
                    │  - ORDER-SERVICE                              │
                    │  - DELIVERY-SERVICE                           │
                    └──────────────────────────────────────────────┘
```

---

## Service Architecture

### Customer Service
**Responsibility:** Customer identity and authentication.

- Handles registration and login
- Generates JWT tokens on successful login
- Stores hashed passwords using BCrypt
- Exposes customer lookup endpoints for Order Service (via Feign)

**Key classes:**
- `CustomerController` — REST endpoints
- `CustomerService` — business logic
- `JwtUtil` — token generation
- `Customer` — JPA entity

---

### Restaurant Service
**Responsibility:** Restaurant and menu management.

- Restaurant owners create and manage restaurants
- Menu items are attached to restaurants
- Exposes restaurant and menu lookup endpoints for Order Service (via Feign)

**Key classes:**
- `RestaurantController` — REST endpoints
- `MenuItemController` — menu management
- `Restaurant`, `MenuItem` — JPA entities

---

### Order Service
**Responsibility:** Order lifecycle management.

- Validates customer and restaurant existence via Feign
- Snapshots customer name and restaurant name at order time
- Calculates order total from menu item prices
- Publishes `OrderPlacedEvent` to RabbitMQ after saving
- Listens for `DeliveryStatusUpdatedEvent` to sync delivery status

**Key classes:**
- `OrderController` — REST endpoints
- `OrderService` — business logic with circuit breakers
- `CustomerClient`, `RestaurantClient` — Feign clients
- `OrderEventListener` — RabbitMQ consumer
- `OrderPlacedEvent`, `OrderCancelledEvent` — published events

---

### Delivery Service
**Responsibility:** Delivery assignment and tracking.

- Listens for `OrderPlacedEvent` and automatically creates a delivery record
- Listens for `OrderCancelledEvent` and cancels the delivery
- Publishes `DeliveryStatusUpdatedEvent` when delivery status changes
- Drivers update delivery status through the REST API

**Key classes:**
- `DeliveryController` — REST endpoints
- `DeliveryService` — business logic
- `OrderEventListener` — RabbitMQ consumer
- `DeliveryStatusUpdatedEvent` — published event

---

## Database Architecture

Each service owns its database exclusively. No service accesses another service's database directly.

```
customer-service  ──▶  customer_db
                         └── customers

restaurant-service ──▶  restaurant_db
                          ├── restaurants
                          └── menu_items

order-service      ──▶  order_db
                          ├── orders
                          └── order_items

delivery-service   ──▶  delivery_db
                          └── deliveries
```

**Why database-per-service?**
- Services can evolve their schema independently
- A schema change in one service does not break others
- Each service can be scaled independently without database contention
- Failures in one database do not cascade to other services

---

## API Gateway

The API Gateway is the single entry point for all client requests. Built with Spring Cloud Gateway MVC 5.0.0 (servlet stack, not reactive).

### Request Flow

```
Client Request
     │
     ▼
RateLimitFilter (Order 1)
     │  checks: POST /api/orders → max 5/min per IP
     │  returns 429 if exceeded
     ▼
JwtAuthFilter (Order 2)
     │  checks: Authorization: Bearer <token>
     │  skips: /api/customers/register, /api/customers/login, /api/restaurants/search/**
     │  returns 401 if missing/invalid
     │  sets SecurityContext if valid
     ▼
Spring Security Filter Chain
     │  checks: anyRequest().authenticated()
     │  returns 403 if SecurityContext not set
     ▼
Gateway Router
     │  resolves service via Eureka load balancer (lb://)
     ▼
Downstream Microservice
```

### Route Configuration

| Route ID           | Path Pattern            | Target Service     |
|--------------------|-------------------------|--------------------|
| customer-service   | /api/customers/**       | lb://customer-service   |
| restaurant-service | /api/restaurants/**     | lb://restaurant-service |
| order-service      | /api/orders/**          | lb://order-service      |
| delivery-service   | /api/deliveries/**      | lb://delivery-service   |

### Public Endpoints (no JWT required)

- `POST /api/customers/register`
- `POST /api/customers/login`
- `GET /api/restaurants/search/**`

---

## Service Discovery

Eureka Server acts as the service registry. All services register on startup and send heartbeats every 30 seconds.

```
Service Startup
     │
     ▼
Register with Eureka
  hostname, IP, port, health URL
     │
     ▼
Eureka stores instance info
     │
     ▼
Other services query Eureka
  "Where is restaurant-service?"
     │
     ▼
Eureka returns instance list
     │
     ▼
Spring Cloud LoadBalancer
  picks an instance (round-robin)
     │
     ▼
Request routed to selected instance
```

---

## Event-Driven Messaging

RabbitMQ handles all asynchronous communication between services using a topic exchange pattern.

### Order Placed Flow

```
Customer places order via API Gateway
          │
          ▼
    Order Service
    saves order to DB
          │
          ▼
    Publishes OrderPlacedEvent
    exchange: order.exchange
    routing key: order.placed
          │
          ▼
    RabbitMQ routes to
    order.placed.queue
          │
          ▼
    Delivery Service
    OrderEventListener.handleOrderPlaced()
          │
          ▼
    Creates Delivery record
    status: PENDING
          │
          ▼
    Delivery appears in delivery_db
```

### Delivery Status Update Flow

```
Driver updates delivery status
          │
          ▼
    Delivery Service
    updates delivery in DB
          │
          ▼
    Publishes DeliveryStatusUpdatedEvent
    exchange: delivery.exchange
    routing key: delivery.status.updated
          │
          ▼
    RabbitMQ routes to
    delivery.status.queue
          │
          ▼
    Order Service
    DeliveryEventListener.handleDeliveryStatusUpdate()
          │
          ▼
    Updates order status to match
```

### Order Cancelled Flow

```
Customer cancels order
          │
          ▼
    Order Service
    updates order status to CANCELLED
          │
          ▼
    Publishes OrderCancelledEvent
    exchange: order.exchange
    routing key: order.cancelled
          │
          ▼
    Delivery Service
    cancels associated delivery record
```

### Dead Letter Queue

Failed messages (after 3 retry attempts) are routed to `order.dlq` and `delivery.dlq` to prevent message loss.

---

## Fault Tolerance

### Circuit Breakers (Resilience4j)

Order Service wraps all Feign calls with circuit breakers:

```
Order Service calls Restaurant Service
          │
          ▼
    Circuit Breaker checks state
          │
    ┌─────┴──────┐
  CLOSED        OPEN
    │              │
    ▼              ▼
  Execute      Return fallback
  Feign call   503 Service Unavailable
    │
    ▼
  Success? → reset failure count
  Failure? → increment failure count
    │
    ▼
  5 failures in sliding window?
    │
    ▼
  Circuit OPENS for 10 seconds
    │
    ▼
  HALF-OPEN: allow 3 test requests
    │
    ▼
  All succeed? → CLOSE circuit
  Any fail?   → OPEN again
```

**Circuit breaker configuration:**

| Setting | Value |
|---|---|
| Sliding window size | 5 requests |
| Failure rate threshold | 50% |
| Wait duration in open state | 10 seconds |
| Permitted calls in half-open | 3 |

### Retry Policy (RabbitMQ)

Failed message processing is retried up to 3 times with a 2-second initial interval before routing to the Dead Letter Queue.

---

## Inter-Service Communication

| From | To | Method | Purpose |
|---|---|---|---|
| Order Service | Customer Service | Feign (sync) | Validate customer exists |
| Order Service | Restaurant Service | Feign (sync) | Validate restaurant + get menu item price |
| Order Service | Delivery Service | RabbitMQ (async) | Notify of new order |
| Delivery Service | Order Service | RabbitMQ (async) | Notify of delivery status change |

**Why sync for validation, async for events?**
- Order placement needs customer and restaurant data immediately to build the order — synchronous Feign is appropriate
- Delivery assignment does not need to block the order response — asynchronous RabbitMQ is appropriate

---

## Security Architecture

```
JWT Token Lifecycle:

1. Customer registers → password hashed with BCrypt → stored in customer_db
2. Customer logs in → password verified → JWT generated by Customer Service
3. JWT contains: username (subject), role (claim), expiration (24h)
4. Client sends JWT in Authorization: Bearer header on every request
5. API Gateway JwtAuthFilter validates JWT signature using shared secret
6. If valid → sets Spring SecurityContext → request proceeds
7. If invalid → returns 401 Unauthorized immediately
8. Downstream services receive the request without re-validating JWT
```

**JWT Secret:** Shared between Customer Service (token generation) and API Gateway (token validation). Must be identical in both `application.yml` files.

---

## Docker Architecture

```
Docker Network: microservices_default
(all containers communicate by service name)

┌─────────────────────────────────────────────────────┐
│                  Docker Compose                      │
│                                                      │
│  postgres        (postgres:16-alpine)                │
│  rabbitmq        (rabbitmq:3-management-alpine)      │
│  eureka-server   (built from ./discovery-server)     │
│  customer-service (built from ./customer service)    │
│  restaurant-service (built from ./restaurant-service)│
│  order-service   (built from ./order service)        │
│  delivery-service (built from ./delivery service)    │
│  api-gateway     (built from ./API gateway)          │
│                                                      │
│  Startup order (via depends_on + healthcheck):       │
│  1. postgres + rabbitmq                              │
│  2. eureka-server (waits for postgres healthy)       │
│  3. microservices (wait for eureka healthy)          │
│  4. api-gateway   (waits for all services started)   │
└─────────────────────────────────────────────────────┘
```

Each service image is built using a multi-stage Dockerfile:
- **Stage 1 (build):** `maven:3.9-eclipse-temurin-21-alpine` — compiles and packages the JAR
- **Stage 2 (runtime):** `eclipse-temurin:21-jre-alpine` — runs the JAR, no Maven tools included
