package com.example.uc7;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsFunctionParameterNames;
import com.vaadin.flow.js.JsSnippets;

/**
 * Custom {@link Action} that posts a message on a named
 * {@code BroadcastChannel}, so every other tab of the same origin that listens
 * on the channel receives it. The message is read from an {@link Action.Input}
 * when the trigger fires. The counterpart of {@link BroadcastChannelTrigger}.
 */
public class BroadcastSendAction extends Action {

    private final String channelName;
    private final Action.Input<String> message;

    public BroadcastSendAction(String channelName,
            Action.Input<String> message) {
        this.channelName = Objects.requireNonNull(channelName, "channelName");
        this.message = Objects.requireNonNull(message, "message");
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsSnippets.of(BroadcastJs.class).send(channelName,
                message.toJs(trigger));
    }

    @JsDefinition
    private interface BroadcastJs extends Serializable {

        @JsExpression("""
                const channel = new BroadcastChannel($0);
                channel.postMessage($1(event));
                channel.close();""")
        @JsFunctionParameterNames("event")
        JsFunction send(String channelName, JsFunction message);
    }
}
