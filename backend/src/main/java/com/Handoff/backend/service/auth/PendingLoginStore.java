package com.Handoff.backend.service.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a Supabase session for a login from an untrusted device until the device PIN is verified,
 * so the auth cookies are only issued after the PIN check passes.
 */
@Component
public class PendingLoginStore {
    static final Duration TTL = Duration.ofMinutes(10);

    private record Entry(SupabaseSession session, Instant expiresAt) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public PendingLoginStore() {
        this(Clock.systemUTC());
    }

    PendingLoginStore(Clock clock) {
        this.clock = clock;
    }

    public void put(String email, String deviceId, SupabaseSession session) {
        entries.put(key(email, deviceId), new Entry(session, clock.instant().plus(TTL)));
    }

    public Optional<SupabaseSession> get(String email, String deviceId) {
        String key = key(email, deviceId);
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (clock.instant().isAfter(entry.expiresAt())) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.session());
    }

    public void remove(String email, String deviceId) {
        entries.remove(key(email, deviceId));
    }

    private static String key(String email, String deviceId) {
        return email + "|" + (deviceId == null ? "" : deviceId);
    }
}
