package com.example.uc7;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts how often the scope playground was opened while this instance existed.
 * Each scope in {@link ScopeCounters} gets its own instance, so the counts show
 * which scope keeps state through which user action.
 */
public class VisitCounter implements Serializable {

    private final AtomicInteger visits = new AtomicInteger();

    public int visit() {
        return visits.incrementAndGet();
    }
}
