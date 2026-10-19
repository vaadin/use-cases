package com.example.uc16;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.MouseEventTrigger;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC16 — Live pointer tracker.
 * <p>
 * A panel listens for {@code pointermove}; two {@link SetPropertyAction
 * SetPropertyActions} wire the trigger's built-in
 * {@link MouseEventTrigger.EventData#clientX} and
 * {@link MouseEventTrigger.EventData#clientY} inputs into Spans' textContent.
 * The trigger fires many times per second; no server round-trip happens.
 * <p>
 * Demonstrates {@link MouseEventTrigger}'s typed handler inputs — the
 * equivalent of UC11's custom {@code PointInput} but with no custom code
 * because clientX/clientY are already exposed on {@code EventData}.
 */
@Route(value = "uc16", layout = MainLayout.class)
@PageTitle("UC16 — Pointer tracker")
@UseCaseDescription("Showing live pointer coordinates without server round-trips")
@Menu(order = 16, title = "UC16 — Pointer tracker")
@StyleSheet("uc16.css")
public class PointerTrackerView extends VerticalLayout {

    public PointerTrackerView() {
        addClassName("uc16-view");
        add(new H1("UC16 — Pointer tracker"));
        add(new Paragraph("Move the mouse, or a finger, inside the box. The "
                + "coordinates below update on every pointermove event, tens "
                + "of times per second, without a server round-trip: "
                + "SetPropertyAction writes the trigger's clientX and clientY "
                + "inputs into the page."));

        Div panel = new Div("Move the pointer here");
        panel.setId("panel");
        panel.addClassName("uc16-panel");

        Span xValue = new Span("?");
        xValue.setId("x");
        xValue.addClassName("uc16-readout");

        Span yValue = new Span("?");
        yValue.setId("y");
        yValue.addClassName("uc16-readout");

        new MouseEventTrigger(panel, "pointermove").triggers(
                new SetPropertyAction<>(xValue, "textContent",
                        MouseEventTrigger.EventData.clientX),
                new SetPropertyAction<>(yValue, "textContent",
                        MouseEventTrigger.EventData.clientY));

        Span line = new Span(new Span("clientX:"), xValue, new Span("clientY:"),
                yValue);
        line.addClassName("uc16-readout-line");

        add(panel, line);
    }
}
