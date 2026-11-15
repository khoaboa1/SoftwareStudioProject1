package com.Handoff.backend.service.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PendingLoginStoreTest {

    private static final SupabaseSession SESSION = new SupabaseSession(
        "access", "refresh", 3600, UUID.fromString("11111111-1111-1111-1111-111111111111"));

    private static final SupabaseSession OTHER_SESSION = new SupabaseSession(
        "access-2", "refresh-2", 3600, UUID.fromString("11111111-1111-1111-1111-111111111111"));

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-14T12:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    private final MutableClock clock = new MutableClock();
    private final PendingLoginStore store = new PendingLoginStore(clock);

    @Test
    void get_returnsHeldSessionOnlyForSameEmailAndDevice() {
        store.put("jane@tulane.edu", "device-2", SESSION);
        assertThat(store.get("jane@tulane.edu", "device-2")).contains(SESSION);
        assertThat(store.get("jane@tulane.edu", "device-3")).isEmpty();
        assertThat(store.get("bob@tulane.edu", "device-2")).isEmpty();
    }

    @Test
    void get_supportsMissingDeviceId() {
        store.put("jane@tulane.edu", null, SESSION);
        assertThat(store.get("jane@tulane.edu", null)).contains(SESSION);
    }

    @Test
    void get_keepsSessionUntilTenMinutesPass() {
        store.put("jane@tulane.edu", "device-2", SESSION);
        clock.advance(Duration.ofMinutes(10));
        assertThat(store.get("jane@tulane.edu", "device-2")).contains(SESSION);
        clock.advance(Duration.ofSeconds(1));
        assertThat(store.get("jane@tulane.edu", "device-2")).isEmpty();
    }

    @Test
    void remove_deletesHeldSession() {
        store.put("jane@tulane.edu", "device-2", SESSION);
        store.remove("jane@tulane.edu", "device-2");
        assertThat(store.get("jane@tulane.edu", "device-2")).isEmpty();
    }

    @Test
    void put_replacesEarlierSessionForSameDevice() {
        store.put("jane@tulane.edu", "device-2", SESSION);
        store.put("jane@tulane.edu", "device-2", OTHER_SESSION);
        assertThat(store.get("jane@tulane.edu", "device-2")).contains(OTHER_SESSION);
    }
}
