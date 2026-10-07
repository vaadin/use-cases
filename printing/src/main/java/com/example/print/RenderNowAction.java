package com.example.print;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;

/**
 * Renders a web component's pending property changes right away.
 * <p>
 * Vaadin's web components are built on Lit, which renders a property change in
 * a microtask, not when the property is set. That is normally invisible, but
 * not while printing from script: {@code window.print()} — what a Print button
 * calls — fires {@code beforeprint} while that script is still running, so no
 * microtask runs before the browser lays out the pages. A property set by an
 * earlier action on the same {@link PrintTrigger} is then printed as if it had
 * never changed: a {@code Details} opened for print stays folded on paper.
 * Wired after those actions, this one renders the change before the layout
 * happens. Printing with Ctrl/Cmd+P does not need it, but is not harmed by it
 * either.
 * <p>
 * Calls Lit's {@code performUpdate()}, which is not part of the components'
 * documented API; see {@code API-GAPS.md}.
 */
public class RenderNowAction extends Action {

    private final Component component;

    /**
     * @param component
     *            the Lit-based component whose pending changes should be
     *            rendered
     */
    public RenderNowAction(Component component) {
        this.component = Objects.requireNonNull(component);
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsFunction.of("$0.performUpdate?.();", component.getElement());
    }
}
