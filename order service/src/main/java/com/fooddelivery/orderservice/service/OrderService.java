package com.fooddelivery.orderservice.service;

import com.fooddelivery.orderservice.config.RabbitConfig;
import com.fooddelivery.orderservice.dto.OrderItemRequest;
import com.fooddelivery.orderservice.dto.OrderResponse;
import com.fooddelivery.orderservice.dto.PlaceOrderRequest;
import com.fooddelivery.orderservice.event.OrderPlacedEvent;
import com.fooddelivery.orderservice.exception.ResourceNotFoundException;
import com.fooddelivery.orderservice.model.Order;
import com.fooddelivery.orderservice.model.OrderItem;
import com.fooddelivery.orderservice.repository.OrderRepository;
import com.fooddelivery.orderservice.client.CustomerClient;
import com.fooddelivery.orderservice.client.RestaurantClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;       // Feign client
    private final RestaurantClient restaurantClient;   // Feign client

    private final AmqpTemplate amqpTemplate;



    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {

        // ---- Fetch restaurant snapshot ----
        RestaurantClient.RestaurantResponseDTO restaurantDTO = restaurantClient.getRestaurantById(request.getRestaurantId());
        if (!restaurantDTO.isActive()) {
            throw new IllegalStateException("Restaurant is not accepting orders");
        }
        log.info("Restaurant found: {}", restaurantDTO);

        // ---- Build order entity with IDs + snapshots ----
        CustomerClient.CustomerResponseDto customer =
                customerClient.getCustomerById(request.getCustomerId());

        Order order = Order.builder()
                .customerId(customer.getId())
                .customerName(customer.getFirstName()+" "+ customer.getLastName())         // snapshot
                .restaurantId(request.getRestaurantId())
                .restaurantName(restaurantDTO.getName())        // snapshot
                .deliveryAddress(request.getDeliveryAddress())
                .specialInstructions(request.getSpecialInstructions())
                .status(Order.OrderStatus.PLACED)
                .createdAt(LocalDateTime.now())
                .estimatedDeliveryTime(LocalDateTime.now()
                        .plusMinutes(restaurantDTO.getEstimatedDeliveryMinutes()))
                .items(new ArrayList<>())
                .build();

        // ---- Process order items ----
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : request.getItems()) {
            RestaurantClient.MenuItemResponseDTO menuItem =
                    restaurantClient.getMenuItemByRestaurant(
                            request.getRestaurantId(),
                            itemReq.getMenuItemId()
                    );
            if (!menuItem.isAvailable()) {
                throw new IllegalStateException("Menu item '" + menuItem.getName() + "' is not available");
            }

            BigDecimal subtotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .menuItemId(menuItem.getId())
                    .itemName(menuItem.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getPrice())
                    .subtotal(subtotal)
                    .specialInstructions(itemReq.getSpecialInstructions())
                    .build();
orderItem.setOrder(order);
            order.getItems().add(orderItem);
            total = total.add(subtotal);
        }

        order.setTotalAmount(total);

        // ---- Save order ----
        Order savedOrder = orderRepository.save(order);
        // ---- TODO: publish OrderPlacedEvent for Delivery Service ----

        // Create event object
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(savedOrder.getId())
                .customerId(savedOrder.getCustomerId())
                .customerName(savedOrder.getCustomerName())
                .restaurantId(savedOrder.getRestaurantId())
                .restaurantName(savedOrder.getRestaurantName())
                .deliveryAddress(savedOrder.getDeliveryAddress())
                .totalAmount(savedOrder.getTotalAmount())
                .items(savedOrder.getItems().stream().map(i ->
                        OrderPlacedEvent.OrderItemEvent.builder()
                                .menuItemId(i.getMenuItemId())
                                .itemName(i.getItemName())
                                .quantity(i.getQuantity())
                                .unitPrice(i.getUnitPrice())
                                .build()
                ).toList())
                .build();

        amqpTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.ORDER_PLACED_KEY, event);
        log.info("OrderPlacedEvent published for order: {}", savedOrder.getId());

        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(OrderResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getRestaurantOrders(Long restaurantId) {
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream().map(OrderResponse::fromEntity).toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getCustomerId().equals(customerId)) {
            throw new IllegalStateException("You can only cancel your own orders");
        }

        if (order.getStatus() != Order.OrderStatus.PLACED
                && order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        OrderPlacedEvent.OrderCancelledEvent cancelEvent = OrderPlacedEvent.OrderCancelledEvent.builder()
                .orderId(saved.getId())
                .customerId(saved.getCustomerId())
                .reason("Cancelled by customer")
                .build();

        amqpTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.ORDER_CANCELLED_KEY, cancelEvent);
        log.info("OrderCancelledEvent published for order: {}", saved.getId());

        return OrderResponse.fromEntity(saved);
        // Publish DeliveryCancelledEvent instead of synchronous call
    }

    @Transactional
    public void updateStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
    }
}