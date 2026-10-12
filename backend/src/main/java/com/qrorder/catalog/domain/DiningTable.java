package com.qrorder.catalog.domain;

import com.qrorder.outlet.domain.Outlet;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Physical table in an outlet. QR codes map to (outlet, table_number).
 */
@Entity
@Table(name = "dining_table",
       uniqueConstraints = @UniqueConstraint(columnNames = {"outlet_id", "table_number"}))
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_id", nullable = false)
    private Outlet outlet;

    @Column(name = "table_number", nullable = false, length = 20)
    private String tableNumber;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DiningTable() {}

    public DiningTable(Outlet outlet, String tableNumber) {
        this.outlet = outlet;
        this.tableNumber = tableNumber;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public Outlet getOutlet() { return outlet; }
    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
