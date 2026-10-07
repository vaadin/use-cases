package com.example.uc11;

import com.example.PreventDefaultAction;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.CallbackAction;
import com.vaadin.flow.component.trigger.internal.MouseEventTrigger;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC11 — Right-click and report pointer coordinates to the server.
 * <p>
 * The panel listens for {@code contextmenu} via a {@link MouseEventTrigger}. A
 * {@link com.example.PreventDefaultAction} suppresses the browser's native
 * context menu, then a {@link CallbackAction} consumes the value produced by
 * the custom {@link PointInput}: that input returns a JS {@code {x, y}} object
 * literal built from {@code event.clientX}/{@code event.clientY}, and Jackson
 * decodes it into a {@link Point} record on the server.
 * <p>
 * Demonstrates a custom Input that produces a structured server-side value.
 */
@Route(value = "uc11", layout = MainLayout.class)
@PageTitle("UC11 — Right-click coords")
@Menu(order = 11, title = "UC11 — Right-click coords")
@StyleSheet("uc11.css")
public class RightClickCoordsView extends VerticalLayout {

    public RightClickCoordsView() {
        addClassName("uc11-view");
        add(new H1("UC11 — Right-click coordinates"));
        add(new Paragraph(
                "Right-click anywhere in the box below. A custom PointInput "
                        + "reads event.clientX/clientY in the trigger handler "
                        + "and the value lands on the server as a Point record."));

        Div panel = new Div("Right-click me");
        panel.setId("panel");
        panel.addClassName("panel");

        Span lastClick = new Span("(no right-click yet)");
        lastClick.setId("last");
        lastClick.addClassName("coords");

        MouseEventTrigger contextMenu = new MouseEventTrigger(panel,
                "contextmenu");
        contextMenu.triggers(new PreventDefaultAction(),
                new CallbackAction<>(Point.class, point -> {
                    lastClick.setText("Last right-click: x=" + point.x() + " y="
                            + point.y());
                }, new PointInput()));

        add(panel, lastClick);
    }
}
