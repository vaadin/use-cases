package com.example.acme;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightsTest {

    @Test
    void nothingWatchingIsNotTheSameAsNothingFound() {
        assertTrue(Insights.isActive(Map.of("instrumentation", "active")));
        assertFalse(Insights.isActive(Map.of("instrumentation", "inactive")),
                "no instrumentation registered — in dev, no license key");
        assertFalse(Insights.isActive(Map.of()),
                "a payload without the flag makes no claim of watching");
        assertFalse(Insights.isActive(null));
    }
}
