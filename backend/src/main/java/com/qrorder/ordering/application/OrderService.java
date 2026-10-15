package com.qrorder.ordering.application;

import com.qrorder.ordering.api.dto.CreateOrderRequest;
import com.qrorder.ordering.api.dto.OrderResponse;

import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey);
    OrderResponse getOrderById(UUID orderId);
}
