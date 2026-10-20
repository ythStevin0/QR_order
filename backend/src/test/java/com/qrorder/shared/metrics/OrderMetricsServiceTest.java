package com.qrorder.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private OrderMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new OrderMetricsService(meterRegistry);
    }

    @Test
    @DisplayName("records order created, paid, and cook duration metrics successfully")
    void testMetricsRecording() {
        metricsService.recordOrderCreated(1L, "DINE_IN");
        metricsService.recordOrderPaid(1L);
        metricsService.recordCookDuration(1L, Duration.ofSeconds(180));

        Counter createdCounter = meterRegistry.find("orders.created.total").counter();
        assertThat(createdCounter).isNotNull();
        assertThat(createdCounter.count()).isEqualTo(1.0);

        Counter paidCounter = meterRegistry.find("orders.paid.total").counter();
        assertThat(paidCounter).isNotNull();
        assertThat(paidCounter.count()).isEqualTo(1.0);

        Timer cookTimer = meterRegistry.find("orders.cook.duration").timer();
        assertThat(cookTimer).isNotNull();
        assertThat(cookTimer.count()).isEqualTo(1L);
    }
}
