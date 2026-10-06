package com.example.print;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;

/**
 * Redraws every chart in a subtree at the width it currently has.
 * <p>
 * Highcharts sizes its SVG once, in pixels, when the chart is drawn. The print
 * stylesheet changes the layout width underneath it and nothing tells the chart
 * to re-measure, so it prints at its screen width — clipped, or spilling onto a
 * second sheet. {@code Chart} has no server-side "redraw now" API that would
 * help either, and the server could not answer in time anyway. Wired to a
 * {@link PrintTrigger}, this action reflows the charts in the browser, on
 * {@code beforeprint} for the paper and on {@code afterprint} for the screen
 * again.
 */
public class ReflowChartsAction extends Action {

    private final Component root;

    /**
     * @param root
     *            the component whose {@code vaadin-chart} descendants should be
     *            reflowed
     */
    public ReflowChartsAction(Component root) {
        this.root = Objects.requireNonNull(root);
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsFunction.of("""
                for (const chart of $0.querySelectorAll('vaadin-chart')) {
                    chart.configuration?.reflow();
                }""", root.getElement());
    }
}
