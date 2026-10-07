package com.example.uc2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC2 — Click an image to dim its siblings atomically.
 * <p>
 * Three {@link Image} tiles. One {@link ClickTrigger} per tile fires three
 * {@link SetPropertyAction} calls in sequence on the same gesture — mark self
 * {@code selected}, set the other two to {@code dimmed} — so the three DOM
 * mutations land before the next paint with no server round-trip between them.
 * A "Reset" button fans out three more actions to clear all classes.
 * <p>
 * The framework analogue would attach three server-side click listeners and
 * call {@code img.getElement().getClassList().add(...)} for each target; every
 * click costs one round-trip and the mid-sequence states (one image selected,
 * the others still pristine) are observable. Triggers ship the whole transition
 * as one atomic client-side handler. The same applies to any
 * non-{@code HasEnabled} target — {@link Image} doesn't have a
 * {@code setEnabled}, but {@link SetPropertyAction} can write any DOM property
 * regardless.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Click an image to dim its siblings")
@Menu(order = 2, title = "UC2 — Image gallery select")
@StyleSheet("uc2.css")
public class SubmitAndDisableView extends VerticalLayout {

    public SubmitAndDisableView() {
        addClassName("uc2-view");
        add(new H1("UC2 — Click an image to dim its siblings"));
        add(new Paragraph(
                "Click one tile: the other two fade to grayscale and the "
                        + "selected one gets a primary border — all three DOM "
                        + "mutations land in one event, no server round-trip "
                        + "between them. Image isn't HasEnabled, but "
                        + "SetPropertyAction writes any property on any "
                        + "element. Reset clears everything in one trigger."));

        Image a = tile("a", "ALPHA", "1976d2");
        Image b = tile("b", "BETA", "d81b60");
        Image c = tile("c", "GAMMA", "388e3c");

        HorizontalLayout gallery = new HorizontalLayout(a, b, c);
        gallery.addClassName("gallery");

        wire(a, b, c);
        wire(b, a, c);
        wire(c, a, b);

        Button reset = new Button("Reset");
        reset.setId("reset");
        new ClickTrigger(reset).triggers(
                new SetPropertyAction<>(a, "className", ""),
                new SetPropertyAction<>(b, "className", ""),
                new SetPropertyAction<>(c, "className", ""));

        add(gallery, reset);
    }

    private static void wire(Image self, Image other1, Image other2) {
        new ClickTrigger(self).triggers(
                new SetPropertyAction<>(self, "className", "selected"),
                new SetPropertyAction<>(other1, "className", "dimmed"),
                new SetPropertyAction<>(other2, "className", "dimmed"));
    }

    private static Image tile(String id, String label, String hexColor) {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' "
                + "viewBox='0 0 200 200'>"
                + "<rect width='200' height='200' fill='#" + hexColor + "'/>"
                + "<text x='100' y='115' text-anchor='middle' fill='white' "
                + "font-size='28' font-family='sans-serif' "
                + "font-weight='600'>" + label + "</text>" + "</svg>";
        String dataUri = "data:image/svg+xml;base64," + Base64.getEncoder()
                .encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        Image img = new Image(dataUri, label);
        img.setId(id);
        return img;
    }
}
