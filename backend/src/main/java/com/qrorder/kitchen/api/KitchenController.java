package com.qrorder.kitchen.api;

import com.qrorder.kitchen.api.dto.UpdateOrderStatusRequest;
import com.qrorder.kitchen.application.KitchenService;
import com.qrorder.ordering.api.dto.OrderResponse;
import com.qrorder.shared.dto.WebResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping("/kitchen/orders")
    public ResponseEntity<WebResponse<List<OrderResponse>>> getActiveOrders(@RequestParam Long outletId) {
        List<OrderResponse> orders = kitchenService.getActiveKitchenOrders(outletId);
        return ResponseEntity.ok(WebResponse.success(orders));
    }

    @GetMapping(value = "/kitchen/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamKitchenOrders(@RequestParam Long outletId) {
        return kitchenService.registerKitchenStream(outletId);
    }

    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<WebResponse<OrderResponse>> updateStatus(
            @PathVariable UUID orderId,
            @RequestBody @Valid UpdateOrderStatusRequest request,
            @RequestHeader(value = "X-Staff-Username", required = false, defaultValue = "KITCHEN_STAFF") String staffUsername
    ) {
        OrderResponse response = kitchenService.updateOrderStatus(orderId, request.status(), staffUsername);
        return ResponseEntity.ok(WebResponse.success(response, "Order status updated successfully"));
    }

    @GetMapping(value = "/orders/{orderId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCustomerOrderStatus(@PathVariable UUID orderId) {
        return kitchenService.registerCustomerStream(orderId);
    }
}
