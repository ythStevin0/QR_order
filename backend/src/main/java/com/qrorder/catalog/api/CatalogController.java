package com.qrorder.catalog.api;

import com.qrorder.catalog.api.dto.MenuItemResponse;
import com.qrorder.catalog.api.dto.TableContextResponse;
import com.qrorder.catalog.application.CatalogService;
import com.qrorder.shared.dto.WebResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/outlets/{outletId}")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<WebResponse<TableContextResponse>> getOutletContext(@PathVariable Long outletId) {
        TableContextResponse context = catalogService.getOutletContext(outletId);
        return ResponseEntity.ok(WebResponse.success(context));
    }

    @GetMapping("/menu")
    public ResponseEntity<WebResponse<List<MenuItemResponse>>> getActiveMenu(@PathVariable Long outletId) {
        List<MenuItemResponse> menu = catalogService.getActiveMenu(outletId);
        return ResponseEntity.ok(WebResponse.success(menu));
    }

    @GetMapping("/tables/{tableNumber}")
    public ResponseEntity<WebResponse<TableContextResponse>> getTableContext(
            @PathVariable Long outletId,
            @PathVariable String tableNumber
    ) {
        TableContextResponse context = catalogService.getTableContext(outletId, tableNumber);
        return ResponseEntity.ok(WebResponse.success(context));
    }
}
