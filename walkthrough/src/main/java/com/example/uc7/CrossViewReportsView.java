package com.example.uc7;

import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * The reports page of UC7 — Tour across views: the last step of the tour is
 * here.
 */
@Route(value = "uc7/reports", layout = MainLayout.class)
@PageTitle("UC7 — Tour across views")
public class CrossViewReportsView extends CrossViewPage {

    static final String REPORT = "report";

    public CrossViewReportsView() {
        super("Reports");
        Div report = new Div(bar("Jan", 40), bar("Feb", 65), bar("Mar", 90));
        report.addClassName("uc7-report");
        addTarget(REPORT, report);
        addContent(report);
    }

    private static Div bar(String month, int percent) {
        Div fill = new Div();
        fill.addClassName("uc7-bar-fill");
        fill.getStyle().setWidth(percent + "%");
        Div bar = new Div(new Span(month), fill);
        bar.addClassName("uc7-bar");
        return bar;
    }
}
