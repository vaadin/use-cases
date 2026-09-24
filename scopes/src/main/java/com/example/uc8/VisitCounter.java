package com.example.uc8;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts how often the scope playground was opened while this instance existed.
 * Each scope in {@link ScopeCounters} gets its own instance, so the counts show
 * which scope keeps state through which user action.
 */
public class VisitCounter implements Serializable {

    private final String instanceId = UUID.randomUUID().toString().substring(0,
            8);
    private final AtomicInteger visits = new AtomicInteger();

    public int visit() {
        return visits.incrementAndGet();
    }

    public int getVisits() {
        return visits.get();
    }

    public String getInstanceId() {
        return instanceId;
    }
}
