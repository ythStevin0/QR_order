package com.qrorder.ordering.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A line item within an order.
 * Price and name are snapshotted at order time — not live references to menu_item.
 */
@Entity
@Table(name = "order_line")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    /** Snapshot of menu item name at order time. */
    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    /** Snapshot of unit price at order time (Rupiah integer). */
    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private int quantity = 1;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderLine() {}

    public OrderLine(Order order, Long menuItemId, String itemName, long unitPrice, int quantity, String notes) {
        this.order = order;
        this.menuItemId = menuItemId;
        this.itemName = itemName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.notes = notes;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // --- Getters ---

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Long getMenuItemId() { return menuItemId; }
    public String getItemName() { return itemName; }
    public long getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}
