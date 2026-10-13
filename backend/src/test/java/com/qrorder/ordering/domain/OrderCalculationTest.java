package com.qrorder.ordering.domain;

import com.qrorder.outlet.domain.Outlet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderCalculationTest {

    @Test
    @DisplayName("Order calculation: Calculates subtotal, tax, service charge, and total in integer Rupiah")
    void calculateTotals() {
        Outlet outlet = new Outlet("Outlet Utama", "Jl. Sudirman", "Asia/Jakarta");
        // Tax rate = 11% (1100 basis points)
        outlet.setTaxRate(1100);
        // Service charge = 5% (500 basis points)
        outlet.setServiceChargeRate(500);

        Order order = new Order(outlet, OrderType.DINE_IN, "05", null);

        // Add 2x Mie Pedas @ Rp 15,000 = Rp 30,000
        order.addLine(1L, "Mie Pedas Level 2", 15_000L, 2, "Level 2");

        // Add 1x Pangsit Goreng @ Rp 12,000 = Rp 12,000
        order.addLine(2L, "Pangsit Goreng", 12_000L, 1, null);

        // Add 2x Es Teh Manis @ Rp 5,000 = Rp 10,000
        order.addLine(3L, "Es Teh Manis", 5_000L, 2, "Es dipisah");

        // Calculate
        order.calculateTotals(outlet.getTaxRate(), outlet.getServiceChargeRate());

        // Subtotal = 30,000 + 12,000 + 10,000 = 52,000
        assertEquals(52_000L, order.getSubtotal());

        // Tax = 52,000 * 1100 / 10000 = 5,720
        assertEquals(5_720L, order.getTaxAmount());

        // Service charge = 52,000 * 500 / 10000 = 2,600
        assertEquals(2_600L, order.getServiceCharge());

        // Total = 52,000 + 5,720 + 2,600 = 60,320
        assertEquals(60_320L, order.getTotal());
    }

    @Test
    @DisplayName("Order calculation: Zero tax and zero service charge")
    void zeroTaxAndServiceCharge() {
        Outlet outlet = new Outlet("Outlet 2", "Jl. Test", "Asia/Jakarta");
        outlet.setTaxRate(0);
        outlet.setServiceChargeRate(0);

        Order order = new Order(outlet, OrderType.TAKE_AWAY, null, "Budi");
        order.addLine(1L, "Mie Gacoan", 20_000L, 1, null);
        order.calculateTotals(outlet.getTaxRate(), outlet.getServiceChargeRate());

        assertEquals(20_000L, order.getSubtotal());
        assertEquals(0L, order.getTaxAmount());
        assertEquals(0L, order.getServiceCharge());
        assertEquals(20_000L, order.getTotal());
    }
}
