package com.qrorder.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Custom application metrics for business observability (PRD §16, Topik 21).
 * Exposes Prometheus-compatible counters, timers, and gauges.
 */
@Service
public class OrderMetricsService {

    private final MeterRegistry meterRegistry;

    public OrderMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordOrderCreated(Long outletId, String orderType) {
        Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .tag("outlet_id", String.valueOf(outletId != null ? outletId : 0))
                .tag("order_type", orderType != null ? orderType : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }

    public void recordOrderPaid(Long outletId) {
        Counter.builder("orders.paid.total")
                .description("Total number of orders paid")
                .tag("outlet_id", String.valueOf(outletId != null ? outletId : 0))
                .register(meterRegistry)
                .increment();
    }

    public void recordOrderCompleted(Long outletId) {
        Counter.builder("orders.completed.total")
                .description("Total number of orders completed")
                .tag("outlet_id", String.valueOf(outletId != null ? outletId : 0))
                .register(meterRegistry)
                .increment();
    }

    public void recordOrderCancelled(Long outletId, String reason) {
        Counter.builder("orders.cancelled.total")
                .description("Total number of orders cancelled")
                .tag("outlet_id", String.valueOf(outletId != null ? outletId : 0))
                .tag("reason", reason != null ? reason : "UNSPECIFIED")
                .register(meterRegistry)
                .increment();
    }

    public void recordCookDuration(Long outletId, Duration duration) {
        Timer.builder("orders.cook.duration")
                .description("Time taken from PAID to READY in the kitchen")
                .tag("outlet_id", String.valueOf(outletId != null ? outletId : 0))
                .publishPercentiles(0.5, 0.9, 0.99)
                .register(meterRegistry)
                .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }
}
