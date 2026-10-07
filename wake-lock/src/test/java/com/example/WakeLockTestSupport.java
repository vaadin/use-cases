package com.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.wakelock.WakeLock;
import com.vaadin.flow.component.wakelock.WakeLockAvailability;

/**
 * Shared helper for browserless tests that need to drive
 * {@link WakeLock#activeSignal()} transitions. In production the active state
 * is driven by the JS bridge; tests drive it through the public
 * {@code UIInternals#setWakeLockActive(boolean)} method.
 */
public final class WakeLockTestSupport {

    private WakeLockTestSupport() {
    }

    public static void simulateState(String state) {
        simulateActive("ACTIVE".equals(state));
    }

    public static void simulateActive(boolean active) {
        UI.getCurrent().getInternals().setWakeLockActive(active);
    }

    public static void simulateAvailability(WakeLockAvailability availability) {
        UI.getCurrent().getInternals().setWakeLockAvailability(availability);
    }

    public static void simulateAcquired() {
        simulateActive(true);
    }

    public static void simulateReleased() {
        simulateActive(false);
    }
}
