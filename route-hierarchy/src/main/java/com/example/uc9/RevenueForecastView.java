package com.example.uc9;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * UC9 — the divergent case: menu parent ≠ route parent.
 * <p>
 * The route is {@code /forecast}, a top-level URL with no relation to
 * {@code uc9}, and there is no {@code @RouteParent}. So the route hierarchy —
 * and with it the breadcrumb above — places this page at the top level. Only
 * {@code @Menu(parent = ReportsView.class)} moves it, and only in the menu
 * tree, where it appears as a child of Reports.
 * <p>
 * Had this used {@code @RouteParent(ReportsView.class)} instead (UC2's
 * approach), the breadcrumb would read Reports › Revenue forecast too. The
 * point of {@code @Menu(parent = ...)} is that it does not.
 */
@Route(value = "forecast", layout = MainLayout.class)
@PageTitle("Revenue forecast")
@Menu(order = 16, title = "Revenue forecast", parent = ReportsView.class)
public class RevenueForecastView extends VerticalLayout {

    public RevenueForecastView() {
        add(new Breadcrumbs());
        add(new H1("Revenue forecast"));
        add(new Paragraph(
                "Note what the trail above does not say: there is no Reports "
                        + "crumb, because this page is not a route child of "
                        + "/uc9 — the URL is /forecast. The menu groups it under "
                        + "Reports anyway, which is exactly what @Menu(parent = "
                        + "...) is for: regrouping navigation without moving the "
                        + "route."));
        add(new RouterLink("← Back to Reports", ReportsView.class));
    }
}
