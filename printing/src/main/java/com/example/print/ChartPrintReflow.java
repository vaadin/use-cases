package com.example.print;

import java.util.UUID;

import com.example.MissingAPI;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;

/**
 * Redraws every chart in a subtree for the paper, and for the screen again
 * afterwards.
 * <p>
 * Highcharts sizes its SVG once, in pixels, when the chart is drawn. The print
 * stylesheet changes the layout width underneath it and nothing tells the
 * chart to re-measure, so it prints at its screen width — clipped, or spilling
 * onto a second sheet. {@code Chart} has no server-side "redraw now" API that
 * would help either, and printing is synchronous on the client: by the time a
 * round trip reached the server, the page would already be rasterised. So the
 * reflow has to happen in the browser, on {@code beforeprint}.
 * <p>
 * Like {@link PrintEvents}, this is an invisible component rather than a
 * one-off call, so that the {@code window} listeners it registers are undone
 * when the view goes away instead of accumulating one pair per visit.
 */
public class ChartPrintReflow extends Div {

    private final String key = "chart-reflow-" + UUID.randomUUID();

    private final Component root;

    /**
     * @param root
     *            the component whose {@code vaadin-chart} descendants should
     *            be reflowed while printing
     */
    public ChartPrintReflow(Component root) {
        this.root = root;
        addClassName("no-print");
        getStyle().set("display", "none");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getElement().executeJs(MissingAPI.withPrintListenerRegistry("""
                const reflow = () => $1.querySelectorAll('vaadin-chart')
                        .forEach(chart => chart.configuration
                                && chart.configuration.reflow());
                window.addEventListener('beforeprint', reflow, { signal });
                window.addEventListener('afterprint', reflow, { signal });
                """), key, root.getElement());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        MissingAPI.abortPrintListeners(detachEvent.getUI(), key);
        super.onDetach(detachEvent);
    }
}
