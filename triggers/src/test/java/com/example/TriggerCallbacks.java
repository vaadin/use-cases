package com.example;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Stands in for the browser half of a trigger in browserless tests, which have
 * no simulator for triggers and actions (API-GAPS.md #7).
 * <p>
 * Actions that report to the server ({@code CallbackAction},
 * {@code SetSignalAction}, the outcome of {@code WriteToClipboardAction})
 * register a return channel on the trigger's host. Invoking that channel with
 * the arguments the browser would send runs the server side of the action.
 */
public final class TriggerCallbacks {

    private TriggerCallbacks() {
    }

    /**
     * Calls the only return channel registered on {@code host} with the given
     * arguments, as the browser does when the trigger fires.
     */
    public static void fire(Component host, Object... arguments) {
        ReturnChannelMap channels = host.getElement().getNode()
                .getFeature(ReturnChannelMap.class);
        ReturnChannelRegistration channel = channels.get(0);
        assertNotNull(channel, "no action reports to the server from "
                + host.getClass().getSimpleName());
        assertNull(channels.get(1), "more than one return channel on "
                + host.getClass().getSimpleName());
        channel.invoke(JacksonUtils.listToJson(List.of(arguments)));
    }

    /**
     * Reports a resolved promise, e.g. a successful clipboard write, to an
     * action that takes success and error callbacks.
     */
    public static void resolve(Component host, Object value) {
        fire(host, JacksonUtils.getMapper().createObjectNode().put("ok", true)
                .set("value", JacksonUtils.getMapper().valueToTree(value)));
    }

    /** The parameters of the JavaScript calls queued for the browser. */
    public static List<Object> pendingJavaScriptParameters() {
        UI ui = UI.getCurrent();
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return ui.getInternals().dumpPendingJavaScriptInvocations().stream()
                .flatMap(pending -> pending.getInvocation().getParameters()
                        .stream())
                .toList();
    }
}
