package com.example.uc8;

import com.example.views.MainLayout;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC8 — Scope playground.
 * <p>
 * Every time this page is opened, it increments one counter per scope. Which
 * counters keep counting and which start over after a reload, a new tab, a
 * duplicated tab, another browser, or navigating away and back shows the
 * lifetime of each scope at a glance.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Scope playground")
@Menu(order = 8, title = "UC8 — Scope playground")
public class ScopePlaygroundView extends VerticalLayout {

    public ScopePlaygroundView(
            @Qualifier("applicationCounter") VisitCounter application,
            @Qualifier("httpSessionCounter") VisitCounter httpSession,
            @Qualifier("vaadinSessionCounter") VisitCounter vaadinSession,
            @Qualifier("browserTabCounter") VisitCounter browserTab,
            @Qualifier("uiCounter") VisitCounter ui,
            @Qualifier("routeCounter") VisitCounter route) {
        add(new H1("UC8 — Scope playground"));
        add(new Paragraph("Each opening of this page counts one visit in "
                + "every scope. Reload, open a new tab, duplicate the tab, "
                + "try another browser, or go to Home and come back, and "
                + "watch which counters continue and which start over."));
        add(new Button("Reload this page",
                e -> UI.getCurrentOrThrow().getPage().reload()));

        add(row("Application (singleton)", application,
                "Shared by all users. Survives everything until the "
                        + "server restarts."));
        add(row("HTTP session (@SessionScope)", httpSession,
                "One per browser session. Also visible to non-Vaadin "
                        + "endpoints."));
        add(row("Vaadin session (@VaadinSessionScope)", vaadinSession,
                "One per browser session, shared by all tabs."));
        add(row("Browser tab (@BrowserTabScope)", browserTab,
                "One per tab. Survives reloads; a duplicated tab shares it."));
        add(row("UI (@UIScope)", ui,
                "One per UI. A reload or a new tab starts over."));
        add(row("Route (@RouteScope)", route,
                "Lives while this view is shown. Navigating away and back "
                        + "starts over; a reload does too."));
    }

    private static Div row(String scope, VisitCounter counter,
            String lifetime) {
        int visits = counter.visit();
        Span value = new Span(visits + (visits == 1 ? " visit" : " visits")
                + " · instance " + counter.getInstanceId());
        value.addClassName("scope-badge");
        Span description = new Span(lifetime);
        description.addClassName("hint");
        UnorderedList details = new UnorderedList(new ListItem(description));
        return new Div(new Span(scope + ": "), value, details);
    }
}
