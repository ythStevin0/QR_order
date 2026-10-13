package com.qrorder.ordering.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Tracks the daily take-away queue counter per outlet (FR-C11).
 * Resets each day per outlet.
 */
@Entity
@Table(name = "daily_counter")
@IdClass(DailyCounterId.class)
public class DailyCounter {

    @Id
    @Column(name = "outlet_id", nullable = false)
    private Long outletId;

    @Id
    @Column(name = "counter_date", nullable = false)
    private LocalDate counterDate;

    @Column(name = "takeaway_count", nullable = false)
    private int takeawayCount = 0;

    protected DailyCounter() {}

    public DailyCounter(Long outletId, LocalDate counterDate) {
        this.outletId = outletId;
        this.counterDate = counterDate;
        this.takeawayCount = 0;
    }

    public int incrementAndGet() {
        this.takeawayCount++;
        return this.takeawayCount;
    }

    public Long getOutletId() { return outletId; }
    public LocalDate getCounterDate() { return counterDate; }
    public int getTakeawayCount() { return takeawayCount; }
}
