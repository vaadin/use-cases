package com.example.ai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyQuotaTest {

    private static final Instant MORNING = Instant
            .parse("2026-10-15T08:00:00Z");

    @Test
    void callsBeyondTheDailyQuotaAreRefusedUntilTheNextDay() {
        MutableClock clock = new MutableClock(MORNING);
        DailyQuota quota = new DailyQuota(2, clock);

        assertTrue(quota.tryAcquire());
        assertTrue(quota.tryAcquire());
        assertFalse(quota.tryAcquire());
        assertEquals(0, quota.remaining());

        clock.now = MORNING.plus(Duration.ofDays(1));
        assertEquals(2, quota.remaining());
        assertTrue(quota.tryAcquire());
    }

    @Test
    void releasedCallsCanBeUsedAgain() {
        DailyQuota quota = new DailyQuota(1, new MutableClock(MORNING));
        assertTrue(quota.tryAcquire());

        quota.release();

        assertTrue(quota.tryAcquire());
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
