package com.example;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Calls {@code target.click()} on a target element, dispatching a synthetic
 * click event. A local stand-in for the {@code ClickAction} that lives on the
 * {@code feature/triggers-actions} branch of vaadin/flow and has not landed in
 * mainline yet. Once the upstream class ships, delete this file and import
 * {@code com.vaadin.flow.component.trigger.internal.ClickAction} instead.
 * <p>
 * Typically used to chain a trigger onto another component's click handling —
 * for example, a shortcut that fires the same path as a button press.
 */
public class ClickAction extends Action {

    private final Element target;

    public ClickAction(Component target) {
        this(Objects.requireNonNull(target).getElement());
    }

    public ClickAction(Element target) {
        this.target = Objects.requireNonNull(target);
    }

    public Element getTarget() {
        return target;
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsFunction.of("$0.click()", target);
    }
}
