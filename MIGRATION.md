# Migration Decision Log

This document explains the architectural decisions made when migrating the Food Delivery Platform from a monolithic Spring Boot application to a microservices architecture.

---

## Table of Contents

- [Why Migrate?](#why-migrate)
- [Service Boundary Decisions](#service-boundary-decisions)
- [Database Decomposition](#database-decomposition)
- [Communication Strategy](#communication-strategy)
- [Security Strategy](#security-strategy)
- [Fault Tolerance Strategy](#fault-tolerance-strategy)
- [API Gateway Decision](#api-gateway-decision)
- [Trade-offs and Known Limitations](#trade-offs-and-known-limitations)

---

## Why Migrate?

The original monolith was a single Spring Boot application with all domains (customers, restaurants, orders, deliveries) packaged together in one deployable unit.

**Problems with the monolith:**

- **Scaling:** The entire application had to be scaled even if only the order service was under heavy load
- **Deployment risk:** A bug in the restaurant module could bring down the entire application
- **Team coupling:** Changes to one domain required coordination with all other domains
- **Technology lock-in:** The entire system was tied to one technology stack and one database

**Goals of the migration:**

- Independent deployability of each domain
- Independent scalability per service
- Fault isolation — one service failing should not bring down others
- Clear ownership boundaries per domain

---

## Service Boundary Decisions

### Why 4 Services?

The monolith was decomposed into 4 services based on **domain-driven design** principles. Each service maps to a distinct business domain with its own data, behaviour, and team ownership.

---

### Customer Service

**Boundary:** Everything related to customer identity and authentication.

**Why separate?**
- Authentication is a cross-cutting concern — it needs to be independently scalable and available
- JWT token issuance is tightly coupled to customer credentials, making it a natural fit here
- Customer data (profiles, addresses) changes at a different rate than order or restaurant data
- In production, this service would be the first to scale under high registration traffic

**What it owns:**
- Customer registration and login
- Password hashing and verification
- JWT token generation
- Customer profile data

**What it does NOT own:**
- Token validation (handled by the API Gateway)
- Order history (owned by Order Service)

---

### Restaurant Service

**Boundary:** Everything related to restaurant and menu management.

**Why separate?**
- Restaurant data is managed by restaurant owners, a completely different user type from customers
- Menu items change frequently (price updates, availability) and need to be updated without affecting order or delivery logic
- Restaurant search is a high-read, low-write workload that benefits from independent scaling

**What it owns:**
- Restaurant profiles and metadata
- Menu items and pricing
- Restaurant availability status

**What it does NOT own:**
- Order validation (Order Service fetches restaurant data via Feign)
- Delivery assignment (owned by Delivery Service)

---

### Order Service

**Boundary:** The order lifecycle from placement to completion.

**Why separate?**
- Orders are the core business transaction — they need to be independently reliable
- Order placement requires data from both Customer and Restaurant services, making it an orchestrator
- Order status needs to be updated asynchronously by the Delivery Service without tight coupling

**What it owns:**
- Order creation and status management
- Order item pricing snapshots (price at time of order)
- Customer name and restaurant name snapshots (denormalized for resilience)

**Why snapshots?**
When an order is placed, the customer name and restaurant name are copied into the order record. This means if a customer changes their name or a restaurant updates its name later, existing orders are unaffected. This is standard practice in event-sourced and microservices systems.

**What it does NOT own:**
- Customer or restaurant data (fetched via Feign at order time)
- Delivery tracking (owned by Delivery Service, communicated via RabbitMQ)

---

### Delivery Service

**Boundary:** Delivery assignment and tracking.

**Why separate?**
- Delivery tracking has its own lifecycle independent of the order (a driver picks up and delivers)
- Delivery status updates are frequent and high-volume — separating this allows it to scale independently
- Delivery logic (driver assignment, routing) can evolve independently from order logic

**What it owns:**
- Delivery records and status
- Timestamps (assigned, picked up, delivered)
- Driver assignment (future)

**What it does NOT own:**
- Order creation (listens for events instead of being called directly)
- Customer or restaurant data (receives snapshots via events)

---

## Database Decomposition

Each service has its own PostgreSQL database. No service queries another service's database directly.

| Service | Database | Key Tables |
|---|---|---|
| Customer Service | customer_db | customers |
| Restaurant Service | restaurant_db | restaurants, menu_items |
| Order Service | order_db | orders, order_items |
| Delivery Service | delivery_db | deliveries |

**Why database-per-service?**

- **Independence:** Services can change their schema without coordinating with other teams
- **Technology freedom:** Each service could use a different database type in future (e.g. Redis for sessions, MongoDB for restaurant search)
- **Failure isolation:** A database failure in one service does not affect others

**Trade-off:**
- No cross-service JOIN queries — data must be fetched via API calls or duplicated via events
- This is intentional — it enforces loose coupling at the data layer

---

## Communication Strategy

Two communication patterns are used depending on the nature of the interaction.

### Synchronous (Feign) — Used for data validation

Order Service calls Customer Service and Restaurant Service synchronously when placing an order because:

- The order cannot be created without knowing the customer exists and is valid
- The order cannot be created without knowing the restaurant is active and the menu item price
- The response is needed immediately to build the order record

**Services using Feign:**
- Order Service → Customer Service (`getCustomerById`)
- Order Service → Restaurant Service (`getRestaurantById`, `getMenuItemByRestaurant`)

### Asynchronous (RabbitMQ) — Used for event notification

Order Service notifies Delivery Service asynchronously via RabbitMQ because:

- Delivery assignment does not need to block the order placement response
- If Delivery Service is temporarily down, the message is queued and processed when it recovers
- This decouples the two services — Order Service does not need to know where Delivery Service is

**Events published:**

| Publisher | Event | Consumer | Trigger |
|---|---|---|---|
| Order Service | OrderPlacedEvent | Delivery Service | New order placed |
| Order Service | OrderCancelledEvent | Delivery Service | Order cancelled |
| Delivery Service | DeliveryStatusUpdatedEvent | Order Service | Delivery status changed |

**Why RabbitMQ over Kafka?**
- RabbitMQ is simpler to set up and operate for a system of this scale
- The message volume does not require Kafka's high-throughput log-based approach
- RabbitMQ's dead letter queue support provides sufficient fault tolerance for failed message processing

---

## Security Strategy

### JWT Authentication at the Gateway

**Decision:** Validate JWT tokens only at the API Gateway, not in individual services.

**Why?**
- Centralizing authentication at the gateway means individual services don't need to know about security — they trust that any request reaching them has already been authenticated
- Adding security to each service would require duplicating JWT validation logic across 4 services
- If the JWT secret changes, only the gateway and Customer Service need to be updated

**How it works:**
1. Customer Service generates JWT tokens on login (it owns the credentials)
2. API Gateway validates JWT tokens on every protected request
3. Gateway sets the Spring SecurityContext after validation
4. Downstream services receive the request without re-validating

**Trade-off:**
- If the gateway is bypassed (direct service access), there is no authentication. In production, services should only be accessible through the gateway (e.g. via internal network rules or service mesh).

---

## Fault Tolerance Strategy

### Circuit Breakers on Feign Calls

**Decision:** Wrap all Feign calls in Order Service with Resilience4j circuit breakers.

**Why?**
- If Restaurant Service goes down, Order Service should return a clean 503 rather than hanging or throwing an unhandled exception
- Without circuit breakers, a slow downstream service causes thread pool exhaustion in the calling service (cascading failure)
- Circuit breakers give downstream services time to recover before retrying

**Configuration chosen:**
- Sliding window of 5 requests — small window appropriate for lab-scale traffic
- 50% failure threshold — opens after half the requests fail
- 10 second wait — short recovery window for development/testing

**Dead Letter Queue for RabbitMQ:**
- Failed message processing retries 3 times with 2 second intervals
- After 3 failures, message is routed to DLQ for manual inspection
- Prevents poison messages from blocking the queue indefinitely

---

## API Gateway Decision

### Why Spring Cloud Gateway MVC?

**Decision:** Use Spring Cloud Gateway MVC (servlet stack) instead of the reactive Gateway.

**Why?**
- The rest of the system uses the servlet stack (Spring MVC) — mixing reactive and servlet stacks adds unnecessary complexity
- Spring Cloud Gateway MVC 5.0.0 supports standard `OncePerRequestFilter` which integrates cleanly with Spring Security
- Reactive gateway would require rewriting filters using `WebFilter` and `Mono`/`Flux` which adds learning overhead

**Key gateway responsibilities:**
- Single entry point for all client traffic
- JWT authentication (one place to update if auth logic changes)
- Rate limiting on order placement (prevents abuse)
- Eureka-based load balanced routing (`lb://service-name`)

---

## Trade-offs and Known Limitations

| Decision | Trade-off |
|---|---|
| Database per service | No cross-service JOINs — data must be fetched via API or duplicated via events |
| JWT validated only at gateway | Direct service access bypasses authentication — mitigated in production by network rules |
| Synchronous Feign for order validation | If Customer or Restaurant Service is down, orders cannot be placed even with circuit breaker fallback |
| Snapshot denormalization in orders | Order records store customer and restaurant names at order time — stale if names change later (intentional) |
| In-memory rate limiting | Rate limit state is lost on gateway restart — in production use Redis-backed rate limiting |
| Single Eureka server | No high availability for service discovery — in production run multiple Eureka instances |
| Shared JWT secret | Secret is in `application.yml` — in production use a secrets manager (Vault, AWS Secrets Manager) |
