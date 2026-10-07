package com.example.uc6;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * UC6 — Layout-wide auto breadcrumbs (root, {@code uc6}).
 * <p>
 * This view adds no breadcrumb of its own — {@link TeamLayout} owns the single
 * shared {@code Breadcrumbs} component, which rebuilds the trail from the route
 * hierarchy on every navigation.
 */
@Route(value = "uc6", layout = TeamLayout.class)
@PageTitle("Dashboard")
@Menu(order = 6, title = "UC6 — Layout-wide breadcrumbs")
public class DashboardView extends VerticalLayout {

    public DashboardView() {
        add(new H1("Dashboard"));
        add(new Paragraph(
                "The breadcrumb and its rebuild counter above live in the "
                        + "parent layout, not in this view. Navigate into the "
                        + "team and a member — the shared Breadcrumbs component "
                        + "updates itself in ROUTER mode, while the counter ticks "
                        + "once per navigation from a Signal.effect subscribed to "
                        + "UI.routerStateSignal()."));
        add(new RouterLink("Open the team →", TeamView.class));
    }
}
