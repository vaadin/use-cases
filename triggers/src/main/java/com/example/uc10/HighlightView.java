package com.example.uc10;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC10 — Parameterised highlight action.
 * <p>
 * Three buttons each fire a {@link HighlightAction} configured with a different
 * colour and duration. Demonstrates how an action's constructor arguments
 * thread through to the rendered JS via captures. The same target is reused by
 * all three; the configuration is per-instance, not per-target.
 */
@Route(value = "uc10", layout = MainLayout.class)
@PageTitle("UC10 — Highlight")
@Menu(order = 10, title = "UC10 — Highlight")
@StyleSheet("uc10.css")
public class HighlightView extends VerticalLayout {

    public HighlightView() {
        addClassName("uc10-view");
        add(new H1("UC10 — Parameterised highlight"));
        add(new Paragraph(
                "Each button flashes the same target box with its configured "
                        + "colour and duration. The action's constructor "
                        + "arguments end up as JS captures — no string "
                        + "concatenation, all values are Jackson-encoded."));

        Div target = new Div("Target");
        target.setId("target");
        target.addClassName("highlight-target");

        Button yellow = new Button("Brief gold (300ms)");
        yellow.setId("gold");
        new ClickTrigger(yellow)
                .triggers(new HighlightAction(target, "gold", 300));

        Button red = new Button("Long red (1.2s)");
        red.setId("red");
        new ClickTrigger(red)
                .triggers(new HighlightAction(target, "tomato", 1200));

        Button blue = new Button("Cyan tap (500ms)");
        blue.setId("cyan");
        new ClickTrigger(blue)
                .triggers(new HighlightAction(target, "cyan", 500));

        add(target);
        add(new HorizontalLayout(yellow, red, blue));
    }
}
