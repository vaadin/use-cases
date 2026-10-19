package com.example;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.screenorientation.ScreenOrientation;
import com.vaadin.flow.component.screenorientation.ScreenOrientationLockErrorCode;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.js.JsCall;

/**
 * Shared helper for browserless tests of the Screen Orientation API, which has
 * no browserless simulator (API-GAPS.md #1).
 * <p>
 * {@link #setScreenOrientation} feeds
 * {@link ScreenOrientation#orientationSignal()} through the same public entry
 * point the client bridge uses. {@link #pendingLocks()} hands out the lock
 * requests a view sent, so a test can answer them as the browser would.
 */
public final class ScreenOrientationTestSupport {

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
            PendingJavaScriptInvocation invocation) {

        public void succeed() {
            ObjectNode result = JacksonUtils.createObjectNode();
            result.put("success", true);
            invocation.complete(result);
        }

        public void fail(ScreenOrientationLockErrorCode code, String message) {
            ObjectNode result = JacksonUtils.createObjectNode();
            result.put("success", false);
            result.put("code", code.name());
            result.put("message", message);
            invocation.complete(result);
        }
    }

    /**
     * Returns the lock requests sent since the last call, in the order the view
     * sent them. Reading the queue empties it, so any other pending JavaScript
     * is dropped too.
     */
    public static List<PendingLock> pendingLocks() {
        UIInternals internals = UI.getCurrent().getInternals();
        internals.getStateTree().runExecutionsBeforeClientResponse();
        List<PendingLock> locks = new ArrayList<>();
        for (PendingJavaScriptInvocation invocation : internals
                .dumpPendingJavaScriptInvocations()) {
            JsCall call = invocation.getInvocation().getJsCall();
            if (call != null && call
                    .definitionType() == ScreenOrientation.ScreenOrientationJs.class
                    && call.methodName().equals("lock")) {
                locks.add(new PendingLock((String) call.arguments().get(0),
                        invocation));
            }
        }
        return locks;
    }
}
