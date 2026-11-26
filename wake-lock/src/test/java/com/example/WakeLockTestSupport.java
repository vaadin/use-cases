package com.example;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.wakelock.WakeLock;
import com.vaadin.flow.component.wakelock.WakeLockAvailability;

/**
 * Plays the browser's part of {@link WakeLock} in browserless tests, which have
 * no wake lock simulator (API-GAPS.md #1).
 * <p>
 * A test starts one with {@link #capture()} before it navigates, typically in a
 * {@code @BeforeEach} method. {@link #answerWakeLockCalls()} then answers the
 * {@code request} and {@code release} calls sent to the browser the way a
 * browser that grants every request would. A test therefore sees the lock only
 * when the view really asked for it.
 */
public final class WakeLockTestSupport {

    /** A {@link WakeLock#request()} sent to the browser. */
    public static final String REQUEST = "request";
    /** A {@link WakeLock#release()} sent to the browser. */
    public static final String RELEASE = "release";

    private final UI ui;
    private final ExecutedJavaScript executed;
    private int answered;

    private WakeLockTestSupport(UI ui) {
        this.ui = ui;
        this.executed = ExecutedJavaScript.capture(ui);
    }

    /**
     * Starts watching the wake lock calls sent to the browser of the current
     * UI.
     *
     * @return the browser side of the wake lock, not null
     */
    public static WakeLockTestSupport capture() {
        return new WakeLockTestSupport(Objects.requireNonNull(UI.getCurrent()));
    }

    /**
     * Takes the wake lock calls sent to the browser since the previous call, in
     * order, and answers them like a browser that grants every request: after a
     * request {@link WakeLock#activeSignal()} is {@code true}, after a release
     * {@code false}. Without calls the state is left as it is.
     *
     * @return the calls, each {@link #REQUEST} or {@link #RELEASE}
     */
    public List<String> answerWakeLockCalls() {
        List<ExecutedJavaScript.Execution> sent = executed.getExecutions();
        List<String> calls = sent.subList(answered, sent.size()).stream()
                .map(execution -> wakeLockCall(execution.getExpression()))
                .filter(Objects::nonNull).toList();
        answered = sent.size();
        if (!calls.isEmpty()) {
            ui.getInternals()
                    .setWakeLockActive(REQUEST.equals(calls.getLast()));
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
