# Food Delivery Platform

A microservices-based food delivery system migrated from a monolithic architecture. The platform handles customer registration, restaurant management, order placement, and delivery tracking using Spring Boot, Spring Cloud, RabbitMQ, and Docker.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Services](#services)
- [Prerequisites](#prerequisites)
- [Running Locally (IntelliJ)](#running-locally-intellij)
- [Running with Docker](#running-with-docker)
- [Environment Configuration](#environment-configuration)
- [Documentation](#documentation)

---

## Architecture Overview

```
Client
  │
  ▼
API Gateway (port 8080)
  │   JWT Authentication
  │   Rate Limiting
  │
  ├──▶ Customer Service  (port 8081)  ──▶ customer_db
  ├──▶ Restaurant Service (port 8082) ──▶ restaurant_db
  ├──▶ Order Service      (port 8083) ──▶ order_db
  └──▶ Delivery Service   (port 8084) ──▶ delivery_db
            │                    │
            └──── RabbitMQ ──────┘
                  (async events)

All services register with Eureka Server (port 8761)
```

For a detailed architecture diagram and message flow, see [ARCHITECTURE.md](ARCHITECTURE.md).

---

## Services

| Service            | Port | Description                                      |
|--------------------|------|--------------------------------------------------|
| Eureka Server      | 8761 | Service discovery and registration               |
| API Gateway        | 8080 | Single entry point, JWT auth, rate limiting      |
| Customer Service   | 8081 | Customer registration, login, JWT token issuance |
| Restaurant Service | 8082 | Restaurant and menu item management              |
| Order Service      | 8083 | Order placement, status tracking                 |
| Delivery Service   | 8084 | Delivery assignment and status updates           |
| PostgreSQL         | 5432 | One database per service                         |
| RabbitMQ           | 5672 | Async event messaging between services           |

---

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 16
- RabbitMQ 3.13
- Docker Desktop (for Docker setup)
- IntelliJ IDEA (for local setup)

---

## Running Locally (IntelliJ)

### 1. Database Setup

Create the four databases in PostgreSQL:

```sql
CREATE DATABASE customer_db;
CREATE DATABASE restaurant_db;
CREATE DATABASE order_db;
CREATE DATABASE delivery_db;
```

### 2. RabbitMQ

Start RabbitMQ locally (default credentials: `guest`/`guest` on port `5672`).

### 3. Start Services in Order

Start each service in IntelliJ in this exact order — wait for each to fully start before starting the next:

1. `discovery-server` — wait for: `Started DiscoveryServerApplication`
2. `customer-service` — wait for: `Registering application CUSTOMER-SERVICE`
3. `restaurant-service` — wait for: `Registering application RESTAURANT-SERVICE`
4. `order-service` — wait for: `Registering application ORDER-SERVICE`
5. `delivery-service` — wait for: `Registering application DELIVERY-SERVICE`
6. `api-gateway` — wait for: `Started ApiGatewayApplication`

### 4. Verify

Open `http://localhost:8761` — all 5 services should appear as UP.

---

## Running with Docker

### 1. Build and Start

From the root `Microservices/` folder:

```bash
docker compose up --build
```

This will:
- Pull PostgreSQL and RabbitMQ images
- Build all 6 service images
- Start everything in the correct dependency order

First build takes 10-15 minutes as Maven downloads all dependencies.

### 2. Verify

- Eureka dashboard: `http://localhost:8761`
- RabbitMQ management: `http://localhost:15672` (guest/guest)
- API Gateway: `http://localhost:8080`

### 3. Stop

```bash
docker compose down
```

To also remove volumes (wipes all database data):

```bash
docker compose down -v
```

---

## Environment Configuration

Each service has two Spring profiles:

| Profile  | Used when        | Database host | Eureka host     | RabbitMQ host |
|----------|------------------|---------------|-----------------|---------------|
| default  | Running locally  | `localhost`   | `localhost`     | `localhost`   |
| docker   | Running in Docker| `postgres`    | `eureka-server` | `rabbitmq`    |

Docker Compose automatically activates the `docker` profile via:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
```

---

## Testing the System

Import the Postman collection from `docs/FoodDelivery.postman_collection.json` and run the full order flow:

1. `POST /api/customers/register` — create a customer
2. `POST /api/customers/login` — get JWT token
3. `POST /api/restaurants` — create a restaurant
4. `POST /api/restaurants/{id}/menu` — add a menu item
5. `POST /api/orders` — place an order
6. `GET /api/deliveries` — verify delivery was auto-assigned

See [API.md](API.md) for full endpoint documentation.

---

## Documentation

| Document | Description |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | System architecture, service boundaries, message flows |
| [API.md](API.md) | Full API contract for all services |
| [MIGRATION.md](MIGRATION.md) | Migration decisions and service boundary rationale |

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Spring Boot | 4.0.3 | Service framework |
| Spring Cloud Gateway MVC | 5.0.0 | API Gateway |
| Spring Cloud Netflix Eureka | 2025.1.0 | Service discovery |
| Spring Cloud OpenFeign | 2025.1.0 | Inter-service HTTP calls |
| Resilience4j | latest | Circuit breakers |
| RabbitMQ | 3.13 | Async messaging |
| PostgreSQL | 16 | Persistent storage |
| Docker | latest | Containerization |
| JJWT | 0.12.5 | JWT token generation and validation |
