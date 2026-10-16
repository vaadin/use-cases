package com.example;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.webnotification.WebNotification;
import com.vaadin.flow.component.webnotification.WebNotificationError;
import com.vaadin.flow.component.webnotification.WebNotificationOptions;
import com.vaadin.flow.component.webnotification.WebNotificationPermission;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

/**
 * Test-side stand-in for the browser half of {@link WebNotification}.
 * Browserless tests never run the client JavaScript, so this helper plays the
 * browser: it reports a permission, picks up the notifications the server asked
 * the browser to show, and reports clicks, closes and failures back through
 * each notification's return channel.
 * <p>
 * Flow has no simulator for web notifications (compare
 * {@code GeolocationSimulator}), so this reaches into framework internals; see
 * API-GAPS.md #6.
 */
public final class WebNotificationSimulator {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** A notification the browser is showing. */
    public record ShownNotification(String title,
            WebNotificationOptions options, String key,
            ReturnChannelRegistration channel) {
    }

    private static final class State {
        private final List<ShownNotification> shown = new ArrayList<>();
    }

    private WebNotificationSimulator() {
    }

    /** Pretends the browser reported the given permission. */
    public static void setPermission(WebNotificationPermission permission) {
        UI.getCurrent().getInternals()
                .setWebNotificationPermissionFromClient(permission.name());
    }

    /**
     * Notifications the browser is showing. Like the browser, refuses to show a
     * notification without permission and replaces a notification with the same
     * tag.
     */
    public static List<ShownNotification> shown() {
        UI ui = UI.getCurrent();
        State state = state(ui);
        // Calls through a JavaScript definition are queued only right before
        // the response is written, which browserless tests never do.
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        for (PendingJavaScriptInvocation pending : ui.getInternals()
                .dumpPendingJavaScriptInvocations()) {
            JavaScriptInvocation invocation = pending.getInvocation();
            String expression = invocation.getExpression();
            List<Object> parameters = invocation.getParameters();
            if (expression.contains("webNotification.show(")) {
                show(ui, state,
                        new ShownNotification((String) parameters.get(0),
                                (WebNotificationOptions) parameters.get(1),
                                (String) parameters.get(2),
                                (ReturnChannelRegistration) parameters.get(3)));
            } else if (expression.contains("webNotification.closeByTag(")) {
                Object tag = parameters.get(0);
                List.copyOf(state.shown).stream()
                        .filter(n -> tag.equals(n.options().tag()))
                        .forEach(n -> close(state, n));
            } else if (expression.contains("webNotification.close(")) {
                Object key = parameters.get(0);
                List.copyOf(state.shown).stream()
                        .filter(n -> key.equals(n.key()))
                        .forEach(n -> close(state, n));
            }
        }
        return List.copyOf(state.shown);
    }

    /**
     * Pretends the user clicked the notification: the browser reports the click
     * and then, because it closes the notification, the close.
     */
    public static void click(ShownNotification notification) {
        report(notification.channel(), "click");
        close(state(UI.getCurrent()), notification);
    }

    /** Pretends the browser failed to show the notification. */
    public static void fail(ShownNotification notification,
            WebNotificationError.Reason reason, String message) {
        state(UI.getCurrent()).shown.remove(notification);
        report(notification.channel(), "error", reason.name(), message);
    }

    private static void show(UI ui, State state,
            ShownNotification notification) {
        if (WebNotification.permissionSignal(ui)
                .peek() != WebNotificationPermission.GRANTED) {
            report(notification.channel(), "error",
                    WebNotificationError.Reason.NOT_ALLOWED.name(),
                    "Notification permission has not been granted");
            return;
        }
        String tag = notification.options().tag();
        if (tag != null) {
            List.copyOf(state.shown).stream()
                    .filter(n -> tag.equals(n.options().tag()))
                    .forEach(n -> close(state, n));
        }
        state.shown.add(notification);
    }

    private static void close(State state, ShownNotification notification) {
        if (state.shown.remove(notification)) {
            report(notification.channel(), "close");
        }
    }

    private static void report(ReturnChannelRegistration channel,
            String... arguments) {
        ArrayNode args = MAPPER.createArrayNode();
        for (String argument : arguments) {
            args.add(argument);
        }
        channel.invoke(args);
    }

    private static State state(UI ui) {
        State state = ComponentUtil.getData(ui, State.class);
        if (state == null) {
            state = new State();
            ComponentUtil.setData(ui, State.class, state);
        }
        return state;
    }
}
