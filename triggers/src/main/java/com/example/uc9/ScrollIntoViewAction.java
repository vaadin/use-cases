package com.example.uc9;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Custom {@link Action} that calls {@code target.scrollIntoView} on a target
 * element when the bound trigger fires. Uses {@code behavior: 'smooth'} and
 * {@code block: 'center'} so the target settles in the middle of the viewport.
 * <p>
 * The simplest possible custom action: capture the target as {@code $0}, call a
 * method on it. No inputs, no outcome.
 */
public class ScrollIntoViewAction extends Action {

    private final Element target;

    public ScrollIntoViewAction(Component target) {
        this(Objects.requireNonNull(target).getElement());
    }

    public ScrollIntoViewAction(Element target) {
        this.target = Objects.requireNonNull(target);
    }

    public Element getTarget() {
        return target;
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsFunction.of(
                "$0.scrollIntoView({behavior:'smooth',block:'center'})",
                target);
    }
}
