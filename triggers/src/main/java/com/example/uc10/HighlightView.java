package com.example.uc10;

import com.example.common.UseCaseDescription;
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
 * UC10 — Highlight.
 * <p>
 * Three buttons flash the same box, each with its own color and duration. All
 * three use one custom {@link HighlightAction} class, configured through its
 * constructor. The action passes the target, the color and the duration to its
 * JavaScript as parameters, which Flow encodes as JSON, so no value is pasted
 * into the script text. The flash runs in the browser, inside the click.
 */
@Route(value = "uc10", layout = MainLayout.class)
@PageTitle("UC10 — Highlight")
@UseCaseDescription("Reusing one configurable highlight action with different colors and durations")
@Menu(order = 10, title = "UC10 — Highlight")
@StyleSheet("uc10.css")
public class HighlightView extends VerticalLayout {

    public HighlightView() {
        addClassName("uc10-view");
        add(new H1("UC10 — Highlight"));
        add(new Paragraph("Each button flashes the box below with its own "
                + "color and duration. All three buttons use the same action "
                + "class with different settings, and the flash runs in the "
                + "browser without a server round-trip."));

        Div target = new Div("Target");
        target.setId("target");
        target.addClassName("uc10-target");

        Button yellow = new Button("Short yellow (300 ms)");
        yellow.setId("yellow");
        new ClickTrigger(yellow).triggers(
                new HighlightAction(target, tint("--aura-yellow"), 300));

        Button red = new Button("Long red (1.2 s)");
        red.setId("red");
        new ClickTrigger(red).triggers(
                new HighlightAction(target, tint("--aura-red"), 1200));

        Button blue = new Button("Blue (500 ms)");
        blue.setId("blue");
        new ClickTrigger(blue).triggers(
                new HighlightAction(target, tint("--aura-blue"), 500));

        HorizontalLayout buttons = new HorizontalLayout(yellow, red, blue);
        buttons.addClassName("uc10-buttons");
        add(target, buttons);
    }

    // A see-through tint of an Aura color keeps the text readable in light
    // and dark mode.
    private static String tint(String auraColor) {
        return "color-mix(in srgb, var(" + auraColor + ") 40%, transparent)";
    }
}
