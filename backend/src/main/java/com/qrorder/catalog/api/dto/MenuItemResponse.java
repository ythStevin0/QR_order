package com.qrorder.catalog.api.dto;

import com.qrorder.catalog.domain.MenuItem;

public record MenuItemResponse(
        Long id,
        String name,
        String category,
        String description,
        long price,
        String imageUrl,
        boolean isAvailable
) {
    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getDescription(),
                item.getPrice(),
                item.getImageUrl(),
                item.isAvailable()
        );
    }
}
