package com.qrorder.outlet.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents a physical restaurant outlet.
 * All business entities reference an outlet (FR-M1).
 */
@Entity
@Table(name = "outlet")
public class Outlet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    private String address;

    @Column(nullable = false, length = 50)
    private String timezone = "Asia/Jakarta";

    /** Tax rate in basis points (e.g. 1100 = 11.00%). */
    @Column(name = "tax_rate", nullable = false)
    private int taxRate = 0;

    /** Service charge rate in basis points. */
    @Column(name = "service_charge_rate", nullable = false)
    private int serviceChargeRate = 0;

    @Column(name = "is_open", nullable = false)
    private boolean open = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Outlet() {}

    public Outlet(String name, String address, String timezone) {
        this.name = name;
        this.address = address;
        this.timezone = timezone;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public int getTaxRate() { return taxRate; }
    public void setTaxRate(int taxRate) { this.taxRate = taxRate; }

    public int getServiceChargeRate() { return serviceChargeRate; }
    public void setServiceChargeRate(int serviceChargeRate) { this.serviceChargeRate = serviceChargeRate; }

    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
