package com.qrorder.ordering.api;

import com.qrorder.ordering.api.dto.CreateOrderRequest;
import com.qrorder.ordering.api.dto.OrderResponse;
import com.qrorder.ordering.application.OrderService;
import com.qrorder.shared.dto.WebResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<WebResponse<OrderResponse>> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid CreateOrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WebResponse.success(response, "Order created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(WebResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<WebResponse<com.qrorder.shared.pagination.CursorPageResponse<OrderResponse>>> getOrdersKeyset(
            @RequestParam Long outletId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor
    ) {
        com.qrorder.shared.pagination.CursorPageResponse<OrderResponse> page = orderService.getOrdersByOutletKeyset(outletId, limit, cursor);
        return ResponseEntity.ok(WebResponse.success(page));
    }
}
