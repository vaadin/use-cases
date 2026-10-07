package com.example.uc7;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * Custom {@link Trigger} that fires when a message arrives on the given named
 * {@code BroadcastChannel}. The channel is created when the trigger installs
 * and closed when it uninstalls.
 * <p>
 * Demonstrates a Trigger whose "event source" isn't a DOM event at all — it's
 * an object the trigger constructs (a {@code BroadcastChannel}) and observes
 * via its {@code message} handler. The host element is only the lifecycle
 * owner; messages from other tabs don't go through it.
 */
public class BroadcastChannelTrigger extends Trigger {

    private final String channelName;

    public BroadcastChannelTrigger(Component host, String channelName) {
        super(host);
        this.channelName = Objects.requireNonNull(channelName, "channelName");
        if (channelName.isEmpty()) {
            throw new IllegalArgumentException("channelName must not be empty");
        }
    }

    @Override
    protected Registration install(JsFunction action) {
        return getHost().addJsInitializer("""
                const channel = new BroadcastChannel($1);
                channel.onmessage = (e) => $0(e);
                return () => { channel.close(); };""", action, channelName);
    }

    /** Inputs exposed by this trigger's handler scope. */
    public abstract static class EventData {
        protected EventData() {
        }

        /**
         * {@code event.data} — the payload posted on the channel by the sending
         * tab. Decoded server-side via Jackson when consumed by an action that
         * decodes its input.
         */
        public static final Action.Input<String> data = new Action.Input<>() {
            @Override
            public JsFunction toJs(Trigger trigger) {
                if (!(trigger instanceof BroadcastChannelTrigger)) {
                    throw new IllegalArgumentException(
                            "Input is scoped to BroadcastChannelTrigger and cannot be used in a "
                                    + trigger.getClass().getSimpleName()
                                    + " handler");
                }
                return JsFunction.of("return event.data")
                        .withArguments("event");
            }
        };
    }
}
