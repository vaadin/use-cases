package com.example.uc19;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Custom action that picks one of three colour pickers' current values based on
 * the host's current width and applies it as the target's background colour.
 * Works for any trigger: if the event carries a {@code width} property (the
 * {@link com.vaadin.flow.component.trigger.internal.SizeTrigger} case) it uses
 * that; otherwise it reads the target's {@code getBoundingClientRect().width},
 * so the same action can be wired to a non-size trigger (e.g. an input event on
 * a colour picker) and still apply the right colour.
 * <p>
 * Demonstrates the pattern that a single Action instance can be wired to
 * multiple Triggers — the action stays a value-less reference; each
 * {@code trigger.triggers(action)} call installs its own registration.
 */
public class ApplyResponsiveColorAction extends Action {

    private final Element target;
    private final Element smallPicker;
    private final Element mediumPicker;
    private final Element largePicker;
    private final int narrowMax;
    private final int wideMin;

    public ApplyResponsiveColorAction(Component target, Component smallPicker,
            Component mediumPicker, Component largePicker, int narrowMax,
            int wideMin) {
        this.target = Objects.requireNonNull(target).getElement();
        this.smallPicker = Objects.requireNonNull(smallPicker).getElement();
        this.mediumPicker = Objects.requireNonNull(mediumPicker).getElement();
        this.largePicker = Objects.requireNonNull(largePicker).getElement();
        if (narrowMax >= wideMin) {
            throw new IllegalArgumentException("narrowMax must be < wideMin");
        }
        this.narrowMax = narrowMax;
        this.wideMin = wideMin;
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsFunction.of("""
                const w = (event && typeof event.width === 'number')
                        ? event.width
                        : $0.getBoundingClientRect().width;
                const c = w < $4 ? $1.value : w >= $5 ? $3.value : $2.value;
                $0.style.backgroundColor = c;""", target, smallPicker,
                mediumPicker, largePicker, narrowMax, wideMin)
                .withArguments("event");
    }
}
