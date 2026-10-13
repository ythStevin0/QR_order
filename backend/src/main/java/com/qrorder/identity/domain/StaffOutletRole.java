package com.qrorder.identity.domain;

import com.qrorder.outlet.domain.Outlet;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * RBAC per outlet (FR-M2).
 * Determines what role a staff member has at a specific outlet.
 */
@Entity
@Table(name = "staff_outlet_role",
       uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "outlet_id", "role"}))
public class StaffOutletRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffUser staffUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_id", nullable = false)
    private Outlet outlet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StaffRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StaffOutletRole() {}

    public StaffOutletRole(StaffUser staffUser, Outlet outlet, StaffRole role) {
        this.staffUser = staffUser;
        this.outlet = outlet;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public StaffUser getStaffUser() { return staffUser; }
    public void setStaffUser(StaffUser staffUser) { this.staffUser = staffUser; }
    public Outlet getOutlet() { return outlet; }
    public StaffRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
