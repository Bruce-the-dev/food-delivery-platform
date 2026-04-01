# API Contract Documentation

All requests go through the API Gateway at `http://localhost:8080`.

Protected endpoints require a JWT token in the `Authorization` header:
```
Authorization: Bearer <token>
```

---

## Table of Contents

- [Customer Service](#customer-service)
- [Restaurant Service](#restaurant-service)
- [Order Service](#order-service)
- [Delivery Service](#delivery-service)
- [Error Responses](#error-responses)

---

## Customer Service

Base path: `/api/customers`

---

### Register Customer

**POST** `/api/customers/register` — Public

**Request Body:**
```json
{
    "username": "divine",
    "password": "password123",
    "email": "divine@example.com",
    "phone": "0241234567",
    "address": "123 Main Street"
}
```

**Response 200:**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "divine",
    "role": "CUSTOMER"
}
```

**Response 400 — username already exists:**
```json
{
    "message": "Username already exists",
    "status": 400
}
```

---

### Login

**POST** `/api/customers/login` — Public

**Request Body:**
```json
{
    "username": "divine",
    "password": "password123"
}
```

**Response 200:**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "divine",
    "role": "CUSTOMER"
}
```

**Response 401 — invalid credentials:**
```json
{
    "message": "Invalid username or password",
    "status": 401
}
```

---

### Get Customer by ID

**GET** `/api/customers/{id}` — Protected

**Response 200:**
```json
{
    "id": 1,
    "username": "divine",
    "email": "divine@example.com",
    "phone": "0241234567",
    "address": "123 Main Street",
    "firstName": "Divine",
    "lastName": "Amali"
}
```

**Response 404:**
```json
{
    "message": "Customer not found",
    "status": 404
}
```

---

## Restaurant Service

Base path: `/api/restaurants`

---

### Create Restaurant

**POST** `/api/restaurants` — Protected

**Query Parameter:** `ownerId` (Long, required)

**Request Body:**
```json
{
    "name": "Test Restaurant",
    "address": "456 Food Street",
    "city": "Accra",
    "cuisineType": "Italian",
    "description": "Very Italiano",
    "phone": "0209876543",
    "estimatedDeliveryMinutes": 23
}
```

**Response 200:**
```json
{
    "id": 1,
    "name": "Test Restaurant",
    "address": "456 Food Street",
    "city": "Accra",
    "cuisineType": "Italian",
    "description": "Very Italiano",
    "phone": "0209876543",
    "estimatedDeliveryMinutes": 23,
    "active": true,
    "rating": 0.0,
    "ownerId": 1,
    "ownerName": null,
    "menuItemCount": 0
}
```

---

### Get Restaurant by ID

**GET** `/api/restaurants/{id}` — Protected

**Response 200:**
```json
{
    "id": 1,
    "name": "Test Restaurant",
    "address": "456 Food Street",
    "city": "Accra",
    "cuisineType": "Italian",
    "description": "Very Italiano",
    "phone": "0209876543",
    "estimatedDeliveryMinutes": 23,
    "active": true,
    "rating": 0.0,
    "ownerId": 1,
    "menuItemCount": 1
}
```

---

### Search Restaurants

**GET** `/api/restaurants/search` — Public

**Query Parameters:** `name`, `cuisineType`, `city` (all optional)

**Response 200:**
```json
[
    {
        "id": 1,
        "name": "Test Restaurant",
        "cuisineType": "Italian",
        "city": "Accra",
        "active": true,
        "rating": 0.0
    }
]
```

---

### Add Menu Item

**POST** `/api/restaurants/{restaurantId}/menu` — Protected

**Query Parameter:** `OwnerUsername` (String, required)

**Request Body:**
```json
{
    "name": "Margherita Pizza",
    "description": "Classic pizza with tomato and mozzarella",
    "price": 15.99,
    "available": true
}
```

**Response 200:**
```json
{
    "id": 1,
    "name": "Margherita Pizza",
    "description": "Classic pizza with tomato and mozzarella",
    "price": 15.99,
    "available": true,
    "restaurantId": 1
}
```

---

### Get Menu Item

**GET** `/api/restaurants/{restaurantId}/menu/{menuItemId}` — Protected

**Response 200:**
```json
{
    "id": 1,
    "name": "Margherita Pizza",
    "description": "Classic pizza with tomato and mozzarella",
    "price": 15.99,
    "available": true,
    "restaurantId": 1
}
```

---

### Get All Menu Items for Restaurant

**GET** `/api/restaurants/{restaurantId}/menu` — Protected

**Response 200:**
```json
[
    {
        "id": 1,
        "name": "Margherita Pizza",
        "price": 15.99,
        "available": true,
        "restaurantId": 1
    }
]
```

---

## Order Service

Base path: `/api/orders`

---

### Place Order

**POST** `/api/orders` — Protected

**Request Body:**
```json
{
    "customerId": 1,
    "restaurantId": 1,
    "deliveryAddress": "123 Test Street",
    "specialInstructions": "no onions",
    "items": [
        {
            "menuItemId": 1,
            "quantity": 2,
            "specialInstructions": "extra cheese"
        }
    ]
}
```

**Response 200:**
```json
{
    "id": 1,
    "customerId": 1,
    "customerName": "Divine Amali",
    "restaurantId": 1,
    "restaurantName": "Test Restaurant",
    "deliveryAddress": "123 Test Street",
    "specialInstructions": "no onions",
    "status": "PLACED",
    "totalAmount": 31.98,
    "createdAt": "2026-03-06T13:32:44",
    "estimatedDeliveryTime": "2026-03-06T13:55:44",
    "items": [
        {
            "id": 1,
            "menuItemId": 1,
            "itemName": "Margherita Pizza",
            "quantity": 2,
            "unitPrice": 15.99,
            "subtotal": 31.98
        }
    ]
}
```

**Response 503 — downstream service unavailable:**
```json
{
    "message": "Restaurant Service is currently unavailable. Please try again later.",
    "timestamp": "2026-03-06T13:32:44",
    "status": 503
}
```

**Rate Limit:** Maximum 5 requests per minute per IP. Exceeding returns:
```json
{
    "error": "Too many requests. Maximum 5 order requests per minute allowed."
}
```

---

### Get Order by ID

**GET** `/api/orders/{id}` — Protected

**Response 200:**
```json
{
    "id": 1,
    "customerId": 1,
    "customerName": "Divine Amali",
    "restaurantId": 1,
    "restaurantName": "Test Restaurant",
    "deliveryAddress": "123 Test Street",
    "status": "PLACED",
    "totalAmount": 31.98,
    "createdAt": "2026-03-06T13:32:44",
    "estimatedDeliveryTime": "2026-03-06T13:55:44",
    "items": []
}
```

---

### Get Orders by Customer

**GET** `/api/orders/customer/{customerId}` — Protected

**Response 200:**
```json
[
    {
        "id": 1,
        "restaurantName": "Test Restaurant",
        "status": "PLACED",
        "totalAmount": 31.98,
        "createdAt": "2026-03-06T13:32:44"
    }
]
```

---

### Cancel Order

**PUT** `/api/orders/{id}/cancel` — Protected

**Response 200:**
```json
{
    "id": 1,
    "status": "CANCELLED",
    "restaurantName": "Test Restaurant",
    "totalAmount": 31.98
}
```

**Response 400 — order already being delivered:**
```json
{
    "message": "Order cannot be cancelled at this stage",
    "status": 400
}
```

---

## Delivery Service

Base path: `/api/deliveries`

---

### Get All Deliveries

**GET** `/api/deliveries` — Protected

**Response 200:**
```json
[
    {
        "id": 1,
        "orderId": 1,
        "customerId": 1,
        "customerName": "Divine Amali",
        "restaurantName": "Test Restaurant",
        "deliveryAddress": "123 Test Street",
        "status": "PENDING",
        "assignedAt": "2026-03-06T13:32:45"
    }
]
```

---

### Get Delivery by ID

**GET** `/api/deliveries/{id}` — Protected

**Response 200:**
```json
{
    "id": 1,
    "orderId": 1,
    "customerId": 1,
    "customerName": "Divine Amali",
    "restaurantName": "Test Restaurant",
    "deliveryAddress": "123 Test Street",
    "status": "PENDING",
    "assignedAt": "2026-03-06T13:32:45",
    "pickedUpAt": null,
    "deliveredAt": null
}
```

---

### Get Delivery by Order ID

**GET** `/api/deliveries/order/{orderId}` — Protected

**Response 200:**
```json
{
    "id": 1,
    "orderId": 1,
    "status": "PENDING",
    "deliveryAddress": "123 Test Street"
}
```

---

### Update Delivery Status

**PUT** `/api/deliveries/{id}/status` — Protected

**Request Body:**
```json
{
    "status": "PICKED_UP"
}
```

**Valid status transitions:**
```
PENDING → ASSIGNED → PICKED_UP → OUT_FOR_DELIVERY → DELIVERED
                   → CANCELLED
```

**Response 200:**
```json
{
    "id": 1,
    "orderId": 1,
    "status": "PICKED_UP",
    "pickedUpAt": "2026-03-06T13:40:00"
}
```

---

## Error Responses

All services return errors in a consistent format:

```json
{
    "message": "Description of what went wrong",
    "timestamp": "2026-03-06T13:32:44",
    "status": 404
}
```

### Common HTTP Status Codes

| Status | Meaning |
|---|---|
| 200 | Success |
| 400 | Bad request — validation error or invalid state |
| 401 | Unauthorized — missing or invalid JWT token |
| 403 | Forbidden — valid token but insufficient permissions |
| 404 | Resource not found |
| 429 | Too many requests — rate limit exceeded |
| 503 | Service unavailable — circuit breaker open |

---

## Full Order Flow

The complete end-to-end flow through the gateway:

```
1. POST /api/customers/register        → create account
2. POST /api/customers/login           → get JWT token
3. POST /api/restaurants               → create restaurant (ownerId param)
4. POST /api/restaurants/1/menu        → add menu item (OwnerUsername param)
5. POST /api/orders                    → place order (triggers delivery assignment)
6. GET  /api/deliveries                → verify delivery record was auto-created
7. PUT  /api/deliveries/1/status       → update delivery status (PICKED_UP)
8. PUT  /api/deliveries/1/status       → update delivery status (DELIVERED)
9. GET  /api/orders/1                  → verify order status updated to DELIVERED
```
