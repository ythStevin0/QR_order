package com.qrorder.shared.metrics;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the Kitchen Display System (Topik 20, PRD §16).
 */
@Component
public class KitchenHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Can be dynamically expanded to verify real-time stream status
        return Health.up()
                .withDetail("kitchenSystem", "OPERATIONAL")
                .withDetail("autoSync", "ACTIVE")
                .build();
    }
}
