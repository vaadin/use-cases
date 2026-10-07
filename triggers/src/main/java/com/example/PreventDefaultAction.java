package com.example;

import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;

/**
 * Calls {@code event.preventDefault()} on the trigger's event. Wire it as the
 * first action in a {@link Trigger#triggers(Action...)} call to suppress the
 * browser's built-in handler — context menu, save dialog, navigation
 * back/forward — for an event the application wants to handle itself.
 * <p>
 * Has no equivalent in mainline; the feature/triggers-actions branch's
 * {@code KeyboardEventTrigger#preventDefault()} chainable method covers the
 * same ground for keyboard events, but no generic action exists. Delete this
 * file once an upstream {@code PreventDefaultAction} (or a chainable builder on
 * the base Trigger) lands.
 */
public final class PreventDefaultAction extends Action {

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsFunction.of("event.preventDefault()").withArguments("event");
    }
}
