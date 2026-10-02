package com.example.uc9;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC9 — the control case: route hierarchy and menu hierarchy agree.
 * <p>
 * This page is a route child of {@code uc9} by URL, so it nests under Reports
 * in the breadcrumb and in the menu tree alike, with no annotation beyond the
 * {@code @Menu} that puts it in the menu at all. Compare
 * {@link RevenueForecastView}, where the two hierarchies deliberately differ.
 */
@Route(value = "uc9/sales", layout = MainLayout.class)
@PageTitle("Sales report")
@Menu(order = 15, title = "Sales report")
public class SalesReportView extends VerticalLayout {

    public SalesReportView() {
        add(new Breadcrumbs());
        add(new H1("Sales report"));
        add(new Paragraph(
                "The trail above reads Reports › Sales report, matching where "
                        + "this page sits in the menu. Nothing had to be "
                        + "declared: the URL /uc9/sales already places it under "
                        + "/uc9."));
    }
}
