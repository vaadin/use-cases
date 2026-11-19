package com.example.uc12;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;

/**
 * Custom {@link Action} that sets an aria-live region's {@code textContent} so
 * a screen reader announces the message without moving keyboard focus.
 * <p>
 * The message is captured at constructor time as a JS literal. Production code
 * would accept an {@code Action.Input<String>} as well so dynamic messages can
 * come from a {@code PropertyInput}.
 * <p>
 * The target should be a hidden element with {@code aria-live="polite"} or
 * {@code "assertive"} — the styling that hides it visually lives in the view's
 * CSS.
 */
public class AnnounceAction extends Action {

    private final Element liveRegion;
    private final String message;

    public AnnounceAction(Component liveRegion, String message) {
        this(Objects.requireNonNull(liveRegion).getElement(), message);
    }

    public AnnounceAction(Element liveRegion, String message) {
        this.liveRegion = Objects.requireNonNull(liveRegion);
        this.message = Objects.requireNonNull(message);
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        // Setting textContent to the same value back-to-back doesn't
        // re-announce; clear-then-set forces the screen reader to read.
        return JsSnippets.of(AnnounceJs.class).announce(liveRegion, message);
    }

    @JsDefinition
    private interface AnnounceJs extends Serializable {

        @JsExpression("$0.textContent='';$0.textContent=$1")
        JsFunction announce(Element liveRegion, String message);
    }
}
