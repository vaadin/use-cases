package com.example.uc11;

import java.io.Serializable;

import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsFunctionParameterNames;
import com.vaadin.flow.js.JsSnippets;

/**
 * Calls {@code event.preventDefault()} on the trigger's event. Wire it as the
 * first action in a {@link Trigger#triggers(Action...)} call to suppress the
 * browser's own handling, such as the context menu, for an event the
 * application handles itself.
 * <p>
 * Flow's {@code DomEventTrigger} and {@code MouseEventTrigger} have no way to
 * call {@code preventDefault()}, so this class is a local stand-in until they
 * do (API-GAPS.md #3). Once Flow has a {@code PreventDefaultAction} or a
 * {@code preventDefault()} option on its triggers, delete this class and use
 * Flow's.
 */
public final class PreventDefaultAction extends Action {

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsSnippets.of(PreventDefaultJs.class).preventDefault();
    }

    @JsDefinition
    private interface PreventDefaultJs extends Serializable {

        @JsExpression("event.preventDefault()")
        @JsFunctionParameterNames("event")
        JsFunction preventDefault();
    }
}
