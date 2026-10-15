package com.qrorder.catalog.api.dto;

import com.qrorder.catalog.domain.DiningTable;
import com.qrorder.outlet.domain.Outlet;

public record TableContextResponse(
        Long outletId,
        String outletName,
        boolean outletIsOpen,
        int taxRate,
        int serviceChargeRate,
        String tableNumber,
        boolean tableIsActive
) {
    public static TableContextResponse from(Outlet outlet, DiningTable table) {
        return new TableContextResponse(
                outlet.getId(),
                outlet.getName(),
                outlet.isOpen(),
                outlet.getTaxRate(),
                outlet.getServiceChargeRate(),
                table != null ? table.getTableNumber() : null,
                table != null && table.isActive()
        );
    }
}
