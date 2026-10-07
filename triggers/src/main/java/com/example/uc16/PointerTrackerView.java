package com.example.uc16;

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
@Menu(order = 16, title = "UC16 — Pointer tracker")
@StyleSheet("uc16.css")
public class PointerTrackerView extends VerticalLayout {

    public PointerTrackerView() {
        addClassName("uc16-view");
        add(new H1("UC16 — Pointer tracker"));
        add(new Paragraph(
                "Move your mouse inside the box. The X and Y coordinates "
                        + "below update on every pointermove event. The trigger "
                        + "fires tens of times per second; SetPropertyAction "
                        + "writes the values straight from the trigger's typed "
                        + "EventData inputs — no server round-trip."));

        Div panel = new Div("Move your mouse here");
        panel.setId("panel");
        panel.addClassName("panel");

        Span xValue = new Span("?");
        xValue.setId("x");
        xValue.addClassName("readout");

        Span yValue = new Span("?");
        yValue.setId("y");
        yValue.addClassName("readout");

        new MouseEventTrigger(panel, "pointermove").triggers(
                new SetPropertyAction<>(xValue, "textContent",
                        MouseEventTrigger.EventData.clientX),
                new SetPropertyAction<>(yValue, "textContent",
                        MouseEventTrigger.EventData.clientY));

        Span line = new Span(new Span("clientX:"), xValue, new Span("clientY:"),
                yValue);
        line.addClassName("readout-line");

        add(panel, line);
    }
}
