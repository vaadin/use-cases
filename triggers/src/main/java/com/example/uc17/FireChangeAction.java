package com.example.uc17;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;

/**
 * Fires a {@code change} event on the target, so that a field synchronizes a
 * value that an earlier action wrote. A text field sends its {@code value} to
 * the server on {@code change}, and a script write does not fire that event
 * (API-GAPS.md #8). Once an action can write a value the server sees, delete
 * this class.
 */
public class FireChangeAction extends Action {

    private final Component target;

    public FireChangeAction(Component target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsFunction.of("$0.dispatchEvent(new Event('change'))",
                target.getElement());
    }
}
