package com.example.uc10;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Custom {@link Action} that briefly tints the target's background with a
 * configurable colour for a configurable duration. The colour and duration are
 * captured as JS arguments — they materialise on the client without any string
 * concatenation in the body.
 * <p>
 * Demonstrates how an action takes <em>server-side configuration</em> at
 * construction and threads it through captures. Same pattern any reusable
 * action library would follow.
 */
public class HighlightAction extends Action {

    private final Element target;
    private final String color;
    private final int durationMs;

    public HighlightAction(Component target, String color, int durationMs) {
        this(Objects.requireNonNull(target).getElement(), color, durationMs);
    }

    public HighlightAction(Element target, String color, int durationMs) {
        this.target = Objects.requireNonNull(target);
        this.color = Objects.requireNonNull(color);
        if (durationMs <= 0) {
            throw new IllegalArgumentException("durationMs must be > 0");
        }
        this.durationMs = durationMs;
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsFunction.of("""
                const t=$0;
                const o=t.style.backgroundColor;
                t.style.transition='background-color 200ms';
                t.style.backgroundColor=$1;
                window.setTimeout(()=>{t.style.backgroundColor=o;},$2);""",
                target, color, durationMs);
    }
}
