package com.qrorder.catalog.application;

import com.qrorder.catalog.api.dto.MenuItemResponse;
import com.qrorder.catalog.api.dto.TableContextResponse;

import java.util.List;

public interface CatalogService {
    List<MenuItemResponse> getActiveMenu(Long outletId);
    TableContextResponse getTableContext(Long outletId, String tableNumber);
    TableContextResponse getOutletContext(Long outletId);
    void evictActiveMenuCache(Long outletId);
}
