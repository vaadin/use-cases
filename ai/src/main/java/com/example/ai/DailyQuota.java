package com.example.ai;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Caps how many times per day a paid AI service is called, for all users of the
 * app together, so a public demo cannot run up the bill. A real application
 * would rather limit per user or per team.
 */
public class DailyQuota {

    private final int perDay;
    private final Clock clock;
    private LocalDate day;
    private int used;

    public DailyQuota(int perDay) {
        this(perDay, Clock.systemDefaultZone());
    }

    DailyQuota(int perDay, Clock clock) {
        this.perDay = perDay;
        this.clock = clock;
        this.day = LocalDate.now(clock);
    }

    /**
     * Uses one call of today's quota.
     *
     * @return {@code false} when today's quota is used up
     */
    public synchronized boolean tryAcquire() {
        resetOnNewDay();
        if (used >= perDay) {
            return false;
        }
        used++;
        return true;
    }

    /**
     * Gives back a call that was acquired but failed, so a failing service does
     * not use up the quota.
     */
    public synchronized void release() {
        if (used > 0) {
            used--;
        }
    }

    public synchronized int remaining() {
        resetOnNewDay();
        return perDay - used;
    }

    public int perDay() {
        return perDay;
    }

    private void resetOnNewDay() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
    }
}
