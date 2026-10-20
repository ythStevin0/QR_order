package com.qrorder.shared.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class RedisRateLimiterTest {

    @Test
    @DisplayName("allows requests within threshold and blocks requests over limit")
    void testRateLimiterAllowedAndBlocked() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valOps = Mockito.mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valOps);

        // 1st request -> count = 1 (Allowed)
        when(valOps.increment("ratelimit:ip_1")).thenReturn(1L);
        RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate);

        boolean allowed1 = limiter.isAllowed("ip_1", 2, 60);
        assertThat(allowed1).isTrue();

        // 3rd request -> count = 3 (Blocked, max is 2)
        when(valOps.increment("ratelimit:ip_1")).thenReturn(3L);
        boolean allowed3 = limiter.isAllowed("ip_1", 2, 60);
        assertThat(allowed3).isFalse();
    }

    @Test
    @DisplayName("falls back gracefully when Redis throws an exception")
    void testFallbackWhenRedisFails() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));

        RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate);

        // Should use in-memory fallback
        assertThat(limiter.isAllowed("fallback_key", 2, 60)).isTrue();
        assertThat(limiter.isAllowed("fallback_key", 2, 60)).isTrue();
        assertThat(limiter.isAllowed("fallback_key", 2, 60)).isFalse(); // 3rd request blocked by fallback
    }
}
