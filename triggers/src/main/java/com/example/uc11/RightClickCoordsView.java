package com.example.uc11;

import com.example.common.UseCaseDescription;
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
 * UC11 — Right-click coords.
 * <p>
 * The panel listens for {@code contextmenu} via a {@link MouseEventTrigger}. A
 * {@link PreventDefaultAction} suppresses the browser's native context menu,
 * then a {@link CallbackAction} consumes the value produced by the custom
 * {@link PointInput}: that input returns a JS {@code {x, y}} object literal
 * built from {@code event.clientX}/{@code event.clientY}, and Jackson decodes
 * it into a {@link Point} record on the server.
 * <p>
 * Demonstrates a custom Input that produces a structured server-side value.
 */
@Route(value = "uc11", layout = MainLayout.class)
@PageTitle("UC11 — Right-click coords")
@UseCaseDescription("Reporting right-click coordinates to the server and replacing the browser menu")
@Menu(order = 11, title = "UC11 — Right-click coords")
@StyleSheet("uc11.css")
public class RightClickCoordsView extends VerticalLayout {

    public RightClickCoordsView() {
        addClassName("uc11-view");
        add(new H1("UC11 — Right-click coords"));
        add(new Paragraph("Right-click anywhere in the box below. The "
                + "browser's own menu does not open. Instead, the position "
                + "of the click is sent to "
                + "the server, which shows it below the box. An application "
                + "would open its own menu there."));

        Div panel = new Div("Right-click me");
        panel.setId("panel");
        panel.addClassName("uc11-panel");

        Span lastClick = new Span("(no right-click yet)");
        lastClick.setId("last");
        lastClick.addClassName("uc11-coords");

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
