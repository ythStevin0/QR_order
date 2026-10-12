package com.qrorder.ordering.domain;

import com.qrorder.outlet.domain.Outlet;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core aggregate: a customer order.
 *
 * Key design decisions:
 * - UUID primary key (not guessable, safe to expose to customers)
 * - @Version for optimistic locking (FR-K4)
 * - All money fields are long (Rupiah integer)
 * - Status transitions enforced via state machine
 * - outlet_id on every order (FR-M1)
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_id", nullable = false)
    private Outlet outlet;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Column(name = "table_number", length = 20)
    private String tableNumber;

    @Column(name = "queue_number")
    private Integer queueNumber;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(nullable = false)
    private long subtotal = 0;

    @Column(name = "tax_amount", nullable = false)
    private long taxAmount = 0;

    @Column(name = "service_charge", nullable = false)
    private long serviceCharge = 0;

    @Column(nullable = false)
    private long total = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    private String notes;

    /** Optimistic locking version — prevents concurrent status overwrites (FR-K4). */
    @Version
    @Column(nullable = false)
    private int version = 0;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {}

    /**
     * Create a new order. ID is generated here (not by the database)
     * so the client can receive it immediately.
     */
    public Order(Outlet outlet, OrderType orderType, String tableNumber, String customerName) {
        this.id = UUID.randomUUID();
        this.outlet = outlet;
        this.orderType = orderType;
        this.tableNumber = tableNumber;
        this.customerName = customerName;
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

    // --- Business methods ---

    /**
     * Add an order line. Price and name are snapshotted from the menu item
     * at order time (not referenced live).
     */
    public void addLine(Long menuItemId, String itemName, long unitPrice, int quantity, String notes) {
        OrderLine line = new OrderLine(this, menuItemId, itemName, unitPrice, quantity, notes);
        this.lines.add(line);
    }

    /**
     * Calculate totals from lines and outlet tax/service charge rates.
     * Must be called after all lines are added and before persisting.
     */
    public void calculateTotals(int taxRateBasisPoints, int serviceChargeRateBasisPoints) {
        this.subtotal = lines.stream()
                .mapToLong(l -> l.getUnitPrice() * l.getQuantity())
                .sum();
        this.taxAmount = subtotal * taxRateBasisPoints / 10_000;
        this.serviceCharge = subtotal * serviceChargeRateBasisPoints / 10_000;
        this.total = subtotal + taxAmount + serviceCharge;
    }

    /**
     * Transition to a new status. Validates the transition and records history.
     *
     * @throws IllegalStateException if the transition is not allowed
     */
    public void transitionTo(OrderStatus newStatus, String changedBy) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from %s to %s".formatted(this.status, newStatus));
        }
        OrderStatusHistory history = new OrderStatusHistory(this, this.status, newStatus, changedBy);
        this.statusHistory.add(history);
        this.status = newStatus;
    }

    // --- Getters ---

    public UUID getId() { return id; }
    public Outlet getOutlet() { return outlet; }
    public OrderType getOrderType() { return orderType; }
    public String getTableNumber() { return tableNumber; }

    public Integer getQueueNumber() { return queueNumber; }
    public void setQueueNumber(Integer queueNumber) { this.queueNumber = queueNumber; }

    public String getCustomerName() { return customerName; }
    public long getSubtotal() { return subtotal; }
    public long getTaxAmount() { return taxAmount; }
    public long getServiceCharge() { return serviceCharge; }
    public long getTotal() { return total; }
    public OrderStatus getStatus() { return status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getVersion() { return version; }
    public List<OrderLine> getLines() { return lines; }
    public List<OrderStatusHistory> getStatusHistory() { return statusHistory; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
