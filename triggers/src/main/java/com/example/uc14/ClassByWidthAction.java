package com.example.uc14;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.SizeTrigger;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Custom {@link Action} that toggles a CSS class on a target element based on
 * which width breakpoint the trigger's reported size falls into. Bound to a
 * {@link SizeTrigger} — the trigger emits a {@code {width, height}} event each
 * time the host resizes; this action reads {@code event.width} and applies the
 * breakpoint that includes it.
 * <p>
 * Demonstrates the headline "container queries for Flow" pattern from the
 * Responsive UI Helpers RFC implemented entirely in terms of triggers and
 * actions — no special API needed.
 */
public class ClassByWidthAction extends Action {

    private final Element target;
    private final int narrowMax;
    private final int wideMin;
    private final String narrowClass;
    private final String mediumClass;
    private final String wideClass;

    /**
     * @param target
     *            the element whose class is toggled
     * @param narrowMax
     *            apply {@code narrowClass} when width &lt; this value
     * @param wideMin
     *            apply {@code wideClass} when width &ge; this value
     * @param narrowClass
     *            class to apply when width &lt; narrowMax
     * @param mediumClass
     *            class to apply when narrowMax &le; width &lt; wideMin
     * @param wideClass
     *            class to apply when width &ge; wideMin
     */
    public ClassByWidthAction(Component target, int narrowMax, int wideMin,
            String narrowClass, String mediumClass, String wideClass) {
        this.target = Objects.requireNonNull(target).getElement();
        if (narrowMax >= wideMin) {
            throw new IllegalArgumentException("narrowMax must be < wideMin");
        }
        this.narrowMax = narrowMax;
        this.wideMin = wideMin;
        this.narrowClass = Objects.requireNonNull(narrowClass);
        this.mediumClass = Objects.requireNonNull(mediumClass);
        this.wideClass = Objects.requireNonNull(wideClass);
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        if (!(trigger instanceof SizeTrigger)) {
            throw new IllegalArgumentException(
                    "ClassByWidthAction is only valid in a SizeTrigger handler");
        }
        return JsFunction
                .of("""
                        const w = event.width;
                        const cls = w < $1 ? $3 : w >= $2 ? $5 : $4;
                        $0.classList.remove($3, $4, $5);
                        $0.classList.add(cls);""", target, narrowMax, wideMin,
                        narrowClass, mediumClass, wideClass)
                .withArguments("event");
    }
}
