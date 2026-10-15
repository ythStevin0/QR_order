package com.qrorder.ordering.api.dto;

import com.qrorder.ordering.domain.OrderLine;

public record OrderLineResponse(
        Long id,
        Long menuItemId,
        String itemName,
        long unitPrice,
        int quantity,
        long subtotal,
        String notes
) {
    public static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(
                line.getId(),
                line.getMenuItemId(),
                line.getItemName(),
                line.getUnitPrice(),
                line.getQuantity(),
                line.getUnitPrice() * line.getQuantity(),
                line.getNotes()
        );
    }
}
