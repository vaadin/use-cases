package com.example.uc4;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC4 — Tour across views.
 * <p>
 * A tour whose steps are on three routes: this dashboard,
 * {@link CrossViewOrdersView} and {@link CrossViewReportsView}. "Next" on the
 * last step of a page navigates to the page of the next step, and the tour
 * continues there. The tour state is kept per browser tab in
 * {@link CrossViewTour}.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Tour across views")
@UseCaseDescription("A tour that continues on the next page")
@Menu(order = 4, title = "UC4 — Tour across views")
public class CrossViewTourView extends CrossViewPage {

    static final String KPIS = "kpis";

    public CrossViewTourView() {
        super("Dashboard");
        Div kpis = new Div(kpi("Open orders", "12"), kpi("Shipped today", "7"),
                kpi("Revenue today", "€4,310"));
        kpis.addClassName("uc4-kpis");
        addTarget(KPIS, kpis);
        addContent(kpis);
    }

    private static Div kpi(String label, String value) {
        Span number = new Span(value);
        number.addClassName("uc4-kpi-value");
        Div kpi = new Div(new Span(label), number);
        kpi.addClassName("uc4-kpi");
        return kpi;
    }
}
