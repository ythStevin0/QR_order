package com.qrorder.shared.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Distributed Rate Limiter & Throttler (Topik 1 & 21, PRD §15).
 * Implements a sliding window rate limiter backed by Redis.
 * Includes automatic in-memory fallback for high availability.
 */
@Component
public class RedisRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);
    private final RedisTemplate<String, Object> redisTemplate;

    // In-memory fallback map if Redis is temporarily unreachable
    private final ConcurrentHashMap<String, AtomicInteger> localFallbackMap = new ConcurrentHashMap<>();

    public RedisRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if a request identified by key is within the rate limit.
     *
     * @param key           Identifier (e.g. "order_rate:192.168.1.1" or "auth:kitchen1")
     * @param maxRequests   Maximum allowed requests in the time window
     * @param windowSeconds Window length in seconds
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        String redisKey = "ratelimit:" + key;
        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }
            if (count != null && count > maxRequests) {
                log.warn("Rate limit exceeded for key={}, count={}, max={}", key, count, maxRequests);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis rate limiter unavailable, applying fallback check [{}]: {}", key, e.getMessage());
            return checkLocalFallback(key, maxRequests);
        }
    }

    private boolean checkLocalFallback(String key, int maxRequests) {
        AtomicInteger counter = localFallbackMap.computeIfAbsent(key, k -> new AtomicInteger(0));
        int val = counter.incrementAndGet();
        if (val > maxRequests) {
            return false;
        }
        return true;
    }
}
