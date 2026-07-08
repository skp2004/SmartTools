package com.smarttools.invoice.security.oauth2;

import com.smarttools.invoice.dto.response.JwtAuthResponse;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for short-lived OAuth2 one-time codes.
 * Each code maps to a JwtAuthResponse and is valid for 2 minutes.
 * Calling consume() removes the code, making it one-time-use only.
 */
public class OAuthCodeStore {

    private static final long TTL_SECONDS = 120; // 2 minutes

    private record Entry(JwtAuthResponse response, Instant expiresAt) {}

    private static final Map<String, Entry> store = new ConcurrentHashMap<>();

    private OAuthCodeStore() {}

    /** Store a JWT response under a one-time code with a 2-minute TTL. */
    public static void store(String code, JwtAuthResponse response) {
        purgeExpired();
        store.put(code, new Entry(response, Instant.now().plusSeconds(TTL_SECONDS)));
    }

    /**
     * Consume a code — returns the response and removes it from the store.
     * Returns null if the code is unknown or expired.
     */
    public static JwtAuthResponse consume(String code) {
        purgeExpired();
        Entry entry = store.remove(code);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return null;
        }
        return entry.response();
    }

    /** Remove all expired entries to prevent memory leaks. */
    private static void purgeExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}
