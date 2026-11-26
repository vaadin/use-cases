package com.example;

import java.util.List;
import java.util.Objects;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.screenorientation.ScreenOrientation;
import com.vaadin.flow.component.screenorientation.ScreenOrientationLockErrorCode;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Shared helper for browserless tests of the Screen Orientation API, which has
 * no browserless simulator (API-GAPS.md #1).
 * <p>
 * {@link #setScreenOrientation} feeds
 * {@link ScreenOrientation#orientationSignal()} through the same public entry
 * point the client bridge uses. A test that answers lock requests starts a
 * capture with {@link #capture()} before it navigates, typically in a
 * {@code @BeforeEach} method; {@link #pendingLocks()} then hands out the lock
 * requests the view sent to the browser, so the test can answer them as the
 * browser would.
 */
public final class ScreenOrientationTestSupport {

    private final ExecutedJavaScript executed;
    private int read;

    private ScreenOrientationTestSupport(UI ui) {
        this.executed = ExecutedJavaScript.capture(ui);
    }

    /**
     * Starts watching the lock requests sent to the browser of the current UI.
     *
     * @return the browser side of the lock requests, not null
     */
    public static ScreenOrientationTestSupport capture() {
        return new ScreenOrientationTestSupport(
                Objects.requireNonNull(UI.getCurrent()));
    }

    public static void setScreenOrientation(ScreenOrientationType orientation,
            int angle) {
        UI.getCurrent().getInternals().setScreenOrientationFromClient(
                orientation.getClientValue(), Integer.toString(angle));
    }

    /**
     * A {@link ScreenOrientation#lock} request waiting for the browser's
     * answer.
     *
     * @param orientation
     *            the requested orientation, as the browser names it
     */
    public record PendingLock(String orientation,
            ExecutedJavaScript.Execution execution) {

        public void succeed() {
            ObjectNode result = JacksonUtils.createObjectNode();
            result.put("success", true);
            execution.resolve(result);
        }

        public void fail(ScreenOrientationLockErrorCode code, String message) {
            ObjectNode result = JacksonUtils.createObjectNode();
            result.put("success", false);
            result.put("code", code.name());
            result.put("message", message);
            execution.resolve(result);
        }
    }

    /**
     * Returns the lock requests sent to the browser since the last call, in the
     * order the view sent them.
     */
    public List<PendingLock> pendingLocks() {
        List<ExecutedJavaScript.Execution> sent = executed.getExecutions();
        List<PendingLock> locks = sent.subList(read, sent.size()).stream()
                .flatMap(execution -> execution.getJsCall().filter(call -> call
                        .definitionType() == ScreenOrientation.ScreenOrientationJs.class
                        && call.methodName().equals("lock"))
                        .map(call -> new PendingLock(
                                (String) call.arguments().get(0), execution))
                        .stream())
                .toList();
        read = sent.size();
        return locks;
    }
}
