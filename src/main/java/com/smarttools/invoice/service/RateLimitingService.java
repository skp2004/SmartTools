package com.smarttools.invoice.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // Cache to hold rate limiting buckets per client IP address
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Creates a rate limiting bucket for an IP address.
     * Max 5 attempts, refilling 5 tokens per minute.
     */
    private Bucket createNewBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build())
            .build();
    }

    /**
     * Checks if a request from the given IP address is allowed.
     * Consumes 1 token from the bucket.
     * Returns true if allowed, false if rate limit is exceeded.
     */
    public boolean tryConsume(String ipAddress) {
        Bucket bucket = cache.computeIfAbsent(ipAddress, k -> createNewBucket());
        return bucket.tryConsume(1);
    }
}
