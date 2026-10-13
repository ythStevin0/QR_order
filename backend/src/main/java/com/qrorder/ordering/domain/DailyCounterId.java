package com.qrorder.ordering.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class DailyCounterId implements Serializable {

    private Long outletId;
    private LocalDate counterDate;

    public DailyCounterId() {}

    public DailyCounterId(Long outletId, LocalDate counterDate) {
        this.outletId = outletId;
        this.counterDate = counterDate;
    }

    public Long getOutletId() { return outletId; }
    public void setOutletId(Long outletId) { this.outletId = outletId; }

    public LocalDate getCounterDate() { return counterDate; }
    public void setCounterDate(LocalDate counterDate) { this.counterDate = counterDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyCounterId that)) return false;
        return Objects.equals(outletId, that.outletId) && Objects.equals(counterDate, that.counterDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(outletId, counterDate);
    }
}
