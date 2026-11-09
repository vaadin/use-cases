package com.example.uc11;

import java.io.Serializable;

import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.MouseEventTrigger;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;

/**
 * Custom {@link Action.Input} that returns {@code {x, y}} read from the
 * surrounding {@link MouseEventTrigger}'s event — Jackson decodes the JSON
 * literal directly into a {@link Point} on the server.
 * <p>
 * Demonstrates two things at once: writing a custom Input, and reading
 * trigger-scoped handler state (the {@code event} parameter) from one. The
 * trigger check rejects use in any non-mouse-event handler at install time, the
 * same protection the built-in handler-scoped inputs apply.
 */
public final class PointInput extends Action.Input<Point> {

    @Override
    public JsFunction toJs(Trigger trigger) {
        if (!(trigger instanceof MouseEventTrigger)) {
            throw new IllegalArgumentException(
                    "PointInput is scoped to MouseEventTrigger and cannot be used in a "
                            + trigger.getClass().getSimpleName() + " handler");
        }
        return JsSnippets.of(PointJs.class).point();
    }

    @JsDefinition
    private interface PointJs extends Serializable {

        @JsExpression("return {x: event.clientX, y: event.clientY}")
        @JsCalledWith("event")
        JsFunction point();
    }
}
