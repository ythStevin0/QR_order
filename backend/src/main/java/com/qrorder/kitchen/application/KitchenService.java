package com.qrorder.kitchen.application;

import com.qrorder.ordering.api.dto.OrderResponse;
import com.qrorder.ordering.domain.OrderStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

public interface KitchenService {
    List<OrderResponse> getActiveKitchenOrders(Long outletId);
    OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus, String changedBy);
    SseEmitter registerKitchenStream(Long outletId);
    SseEmitter registerCustomerStream(UUID orderId);
}
