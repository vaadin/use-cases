package com.example.uc9;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC9 — Scroll into view.
 * <p>
 * Two parallel rows of buttons let you compare two ways to do the same thing:
 * <ul>
 * <li><b>Client row</b> wires each button through a
 * {@link ScrollIntoViewAction} on a {@link ClickTrigger}. The scroll fires
 * synchronously inside the click handler — no server round-trip.</li>
 * <li><b>Server row</b> uses a regular server-side {@code addClickListener}
 * that calls {@code Element#executeJs} to scroll. The click goes to the server,
 * the server emits JS back, then the scroll starts — with the latency that
 * brings.</li>
 * </ul>
 * On a fast local connection the difference is small; on a slow or congested
 * one (or with throttled DevTools network) the client-side path stays smooth
 * while the server-side path stutters.
 */
@Route(value = "uc9", layout = MainLayout.class)
@PageTitle("UC9 — Scroll into view")
@UseCaseDescription("Scrolling a target into view on click, client-side or server-side")
@Menu(order = 9, title = "UC9 — Scroll into view")
@StyleSheet("uc9.css")
public class ScrollIntoViewView extends VerticalLayout {

    public ScrollIntoViewView() {
        addClassName("uc9-view");
        add(new H1("UC9 — Scroll into view"));
        add(new Paragraph("Each button scrolls to a section further down. "
                + "The Client row scrolls in the browser, inside the click. "
                + "The Server row sends the click to the server, which then "
                + "tells the browser to scroll. Slow down the network in the "
                + "browser's developer tools to see the difference."));

        Div sectionA = new Div("Section A");
        sectionA.setId("section-a");
        sectionA.addClassName("uc9-section");

        Div sectionB = new Div("Section B");
        sectionB.setId("section-b");
        sectionB.addClassName("uc9-section");

        Div sectionC = new Div("Section C");
        sectionC.setId("section-c");
        sectionC.addClassName("uc9-section");

        HorizontalLayout clientRow = new HorizontalLayout();
        clientRow.addClassName("uc9-row");
        clientRow.add(rowLabel("Client (trigger)"));
        clientRow.add(clientButton("client-a", "→ A", sectionA));
        clientRow.add(clientButton("client-b", "→ B", sectionB));
        clientRow.add(clientButton("client-c", "→ C", sectionC));

        HorizontalLayout serverRow = new HorizontalLayout();
        serverRow.addClassName("uc9-row");
        serverRow.add(rowLabel("Server (round-trip)"));
        serverRow.add(serverButton("server-a", "→ A", sectionA));
        serverRow.add(serverButton("server-b", "→ B", sectionB));
        serverRow.add(serverButton("server-c", "→ C", sectionC));

        add(clientRow, serverRow);
        add(new Div(sectionA, sectionB, sectionC));
    }

    private static Span rowLabel(String text) {
        Span label = new Span(text);
        label.addClassName("uc9-row-label");
        return label;
    }

    private static Button clientButton(String id, String label, Div target) {
        Button button = new Button(label);
        button.setId(id);
        new ClickTrigger(button).triggers(new ScrollIntoViewAction(target));
        return button;
    }

    private static Button serverButton(String id, String label, Div target) {
        Button button = new Button(label);
        button.setId(id);
        Element targetElement = target.getElement();
        button.addClickListener(e -> targetElement.executeJs(
                "this.scrollIntoView({behavior:'smooth',block:'center'})"));
        return button;
    }
}
