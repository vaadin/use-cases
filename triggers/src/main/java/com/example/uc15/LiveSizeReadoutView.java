package com.example.uc15;

import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;
import com.vaadin.flow.component.trigger.internal.SizeTrigger;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC15 — Live size readout.
 * <p>
 * A {@link SizeTrigger} on the view fires whenever its size changes. Two
 * {@link SetPropertyAction}s wire the trigger's built-in
 * {@link SizeTrigger.EventData#width} and {@link SizeTrigger.EventData#height}
 * inputs straight into Spans' {@code textContent} — no callback, no Java
 * handler.
 * <p>
 * Demonstrates the {@code SetPropertyAction(Component, String, Input)}
 * constructor: the value to assign is read at fire time from a typed input
 * source instead of a constant.
 */
@Route(value = "uc15", layout = MainLayout.class)
@PageTitle("UC15 — Live size readout")
@Menu(order = 15, title = "UC15 — Live size readout")
@StyleSheet("uc15.css")
public class LiveSizeReadoutView extends VerticalLayout {

    public LiveSizeReadoutView() {
        addClassName("uc15-view");
        add(new H1("UC15 — Live size readout"));
        add(new Paragraph(
                "Resize the window. The readout below updates without a "
                        + "server round-trip — SizeTrigger fires on every "
                        + "ResizeObserver tick, and SetPropertyAction wires the "
                        + "width and height directly into the Spans' "
                        + "textContent. No callback, no Java handler."));

        Span widthValue = new Span("?");
        widthValue.setId("width");
        widthValue.addClassName("readout");

        Span heightValue = new Span("?");
        heightValue.setId("height");
        heightValue.addClassName("readout");

        new SizeTrigger(this).triggers(
                new SetPropertyAction<>(widthValue, "textContent",
                        SizeTrigger.EventData.width),
                new SetPropertyAction<>(heightValue, "textContent",
                        SizeTrigger.EventData.height));

        Span widthLabel = new Span("Width:");
        widthLabel.addClassName("label");
        Span heightLabel = new Span("Height:");
        heightLabel.addClassName("label");

        Span widthLine = new Span(widthLabel, widthValue, new Span("px"));
        widthLine.addClassName("line");
        Span heightLine = new Span(heightLabel, heightValue, new Span("px"));
        heightLine.addClassName("line");

        add(widthLine, heightLine);
    }
}
