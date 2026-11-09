package com.example.uc6;

import com.example.common.UseCaseDescription;
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
 * UC6 — Network status.
 * <p>
 * The badge changes between "Online" and "Offline" in the browser alone,
 * because the server can not be reached once the network is gone. A custom
 * {@link NetworkStatusTrigger} fires on the browser's {@code online} and
 * {@code offline} window events; a custom {@link ApplyNetworkStatusAction}
 * reads {@code navigator.onLine} at fire time and updates the badge's text and
 * class.
 * <p>
 * To try it, switch the browser's developer tools to "Offline".
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Network status")
@UseCaseDescription("Showing online and offline status even when the server is unreachable")
@Menu(order = 6, title = "UC6 — Network status")
@StyleSheet("uc6.css")
public class NetworkStatusView extends VerticalLayout {

    public NetworkStatusView() {
        addClassName("uc6-view");
        add(new H1("UC6 — Network status"));
        add(new Paragraph("The badge shows whether the browser is online. "
                + "Turn on \"Offline\" in the Network panel of the browser's "
                + "developer tools, or turn off the network: the badge "
                + "changes at once, without the server, which can not be "
                + "reached any more."));

        Span badge = new Span("…");
        badge.setId("status");
        badge.addClassName("uc6-status");

        new NetworkStatusTrigger(this)
                .triggers(new ApplyNetworkStatusAction(badge));

        add(badge);
    }
}
