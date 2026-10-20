package com.qrorder.ordering.application;

import com.qrorder.ordering.api.dto.CreateOrderRequest;
import com.qrorder.ordering.api.dto.OrderResponse;

import com.qrorder.shared.pagination.CursorPageResponse;

import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey);
    OrderResponse getOrderById(UUID orderId);
    CursorPageResponse<OrderResponse> getOrdersByOutletKeyset(Long outletId, int limit, String cursor);
}
