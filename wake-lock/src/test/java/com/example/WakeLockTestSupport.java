package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.browserless.BrowserlessTestConfig;
import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.browserless.JavaScriptHandler;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.wakelock.WakeLock;
import com.vaadin.flow.component.wakelock.WakeLockAvailability;

/**
 * Plays the browser's part of {@link WakeLock} in browserless tests, which have
 * no wake lock simulator (API-GAPS.md #1).
 * <p>
 * {@link Browser} records the {@code request} and {@code release} calls the
 * server sends to the browser, and {@link #answerWakeLockCalls()} answers them
 * the way a browser that grants every request would. A test therefore sees the
 * lock only when the view really asked for it. A test class registers the
 * recorder with
 * {@code @BrowserlessTestConfig(javaScriptHandlers = WakeLockTestSupport.Browser.class)}.
 */
public final class WakeLockTestSupport {

    /** A {@link WakeLock#request()} sent to the browser. */
    public static final String REQUEST = "request";
    /** A {@link WakeLock#release()} sent to the browser. */
    public static final String RELEASE = "release";

    /**
     * Records the wake lock calls sent to the browser of each UI, so that
     * {@link #answerWakeLockCalls()} can answer them when the test decides the
     * browser does. Register it with {@link BrowserlessTestConfig}.
     */
    public static final class Browser implements JavaScriptHandler {

        @Override
        public void handle(ExecutedJavaScript.Execution execution) {
            String call = wakeLockCall(execution.getExpression());
            if (call != null) {
                received(execution.getUI()).add(call);
            }
        }
    }

    private WakeLockTestSupport() {
    }

    /**
     * Takes the wake lock calls sent to the browser since the previous call, in
     * order, and answers them like a browser that grants every request: after a
     * request {@link WakeLock#activeSignal()} is {@code true}, after a release
     * {@code false}. Without calls the state is left as it is.
     *
     * @return the calls, each {@link #REQUEST} or {@link #RELEASE}
     */
    public static List<String> answerWakeLockCalls() {
        UI ui = Objects.requireNonNull(UI.getCurrent());
        // Starting a capture sends whatever is still pending to the browser
        ExecutedJavaScript.capture(ui).stop();
        List<String> received = received(ui);
        List<String> calls = List.copyOf(received);
        received.clear();
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

    @SuppressWarnings("unchecked")
    private static List<String> received(UI ui) {
        List<String> received = (List<String>) ComponentUtil.getData(ui,
                Browser.class.getName());
        if (received == null) {
            received = new ArrayList<>();
            ComponentUtil.setData(ui, Browser.class.getName(), received);
        }
        return received;
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
