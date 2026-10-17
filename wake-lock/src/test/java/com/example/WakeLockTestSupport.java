package com.example;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.wakelock.WakeLock;
import com.vaadin.flow.component.wakelock.WakeLockAvailability;

/**
 * Plays the browser's part of {@link WakeLock} in browserless tests, which have
 * no wake lock simulator (API-GAPS.md #1).
 * <p>
 * {@link #answerWakeLockCalls()} reads the {@code request} and {@code release}
 * calls the server queued for the browser, and answers them the way a browser
 * that grants every request would. A test therefore sees the lock only when the
 * view really asked for it.
 */
public final class WakeLockTestSupport {

    /** A {@link WakeLock#request()} queued for the browser. */
    public static final String REQUEST = "request";
    /** A {@link WakeLock#release()} queued for the browser. */
    public static final String RELEASE = "release";

    private WakeLockTestSupport() {
    }

    /**
     * Takes the wake lock calls queued for the browser since the previous round
     * trip, in order, and answers them like a browser that grants every
     * request: after a request {@link WakeLock#activeSignal()} is {@code true},
     * after a release {@code false}. Without calls the state is left as it is.
     * <p>
     * Takes the whole JavaScript queue, the way writing the response would, so
     * call it once per round trip.
     *
     * @return the calls, each {@link #REQUEST} or {@link #RELEASE}
     */
    public static List<String> answerWakeLockCalls() {
        UIInternals internals = Objects.requireNonNull(UI.getCurrent())
                .getInternals();
        // executeJs calls are queued only when the response is written
        internals.getStateTree().runExecutionsBeforeClientResponse();
        List<String> calls = internals.dumpPendingJavaScriptInvocations()
                .stream()
                .map(pending -> pending.getInvocation().getExpression())
                .map(WakeLockTestSupport::wakeLockCall).filter(Objects::nonNull)
                .toList();
        if (!calls.isEmpty()) {
            internals.setWakeLockActive(REQUEST.equals(calls.getLast()));
        }
        return calls;
    }

    /**
     * Reports what the browser said about the API, as the bootstrap would.
     *
     * @param availability
     *            the availability to report
     */
    public static void simulateAvailability(WakeLockAvailability availability) {
        Objects.requireNonNull(UI.getCurrent()).getInternals()
                .setWakeLockAvailability(availability);
    }

    private static @Nullable String wakeLockCall(String expression) {
        if (expression.contains("Vaadin.Flow.wakeLock.request(")) {
            return REQUEST;
        }
        if (expression.contains("Vaadin.Flow.wakeLock.release(")) {
            return RELEASE;
        }
        return null;
    }
}
