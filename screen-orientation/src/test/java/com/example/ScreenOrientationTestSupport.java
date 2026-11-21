package com.example;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.BrowserlessTestConfig;
import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.browserless.JavaScriptHandler;
import com.vaadin.flow.component.ComponentUtil;
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
 * point the client bridge uses. {@link Browser} records the lock requests a
 * view sends to the browser, and {@link #pendingLocks()} hands them out, so a
 * test can answer them as the browser would. A test class that answers locks
 * registers the recorder with
 * {@code @BrowserlessTestConfig(javaScriptHandlers = ScreenOrientationTestSupport.Browser.class)}.
 */
public final class ScreenOrientationTestSupport {

    /**
     * Records the {@link ScreenOrientation#lock} requests sent to the browser
     * of each UI, so that {@link #pendingLocks()} can hand them out. Register
     * it with {@link BrowserlessTestConfig}.
     */
    public static final class Browser implements JavaScriptHandler {

        @Override
        public void handle(ExecutedJavaScript.Execution execution) {
            execution.getJsCall().filter(call -> call
                    .definitionType() == ScreenOrientation.ScreenOrientationJs.class
                    && call.methodName().equals("lock"))
                    .ifPresent(call -> received(execution.getUI()).add(
                            new PendingLock((String) call.arguments().get(0),
                                    execution)));
        }
    }

    private ScreenOrientationTestSupport() {
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
    public static List<PendingLock> pendingLocks() {
        UI ui = UI.getCurrent();
        // Starting a capture sends whatever is still pending to the browser
        ExecutedJavaScript.capture(ui).stop();
        List<PendingLock> received = received(ui);
        List<PendingLock> locks = List.copyOf(received);
        received.clear();
        return locks;
    }

    @SuppressWarnings("unchecked")
    private static List<PendingLock> received(UI ui) {
        List<PendingLock> received = (List<PendingLock>) ComponentUtil
                .getData(ui, Browser.class.getName());
        if (received == null) {
            received = new ArrayList<>();
            ComponentUtil.setData(ui, Browser.class.getName(), received);
        }
        return received;
    }
}
