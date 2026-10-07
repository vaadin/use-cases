package com.example.uc6;

import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC6 — Network status badge.
 * <p>
 * The status flips between "Online" and "Offline" purely client-side because a
 * server callback wouldn't be reachable when the network drops. A custom
 * {@link NetworkStatusTrigger} fires on the browser's {@code online} and
 * {@code offline} window events; a custom {@link ApplyNetworkStatusAction}
 * reads {@code navigator.onLine} at fire time and updates the badge's text and
 * class.
 * <p>
 * To test: open DevTools' Network panel and toggle "Offline".
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Network status")
@Menu(order = 6, title = "UC6 — Network status")
@StyleSheet("uc6.css")
public class NetworkStatusView extends VerticalLayout {

    public NetworkStatusView() {
        addClassName("uc6-view");
        add(new H1("UC6 — Network status"));
        add(new Paragraph(
                "The badge below tracks the browser's online/offline state. "
                        + "Toggle \"Offline\" in DevTools' Network panel; the badge "
                        + "flips with no server round-trip."));

        Span badge = new Span("…");
        badge.setId("status");
        badge.addClassName("status-badge");

        new NetworkStatusTrigger(this)
                .triggers(new ApplyNetworkStatusAction(badge));

        add(badge);
    }
}
