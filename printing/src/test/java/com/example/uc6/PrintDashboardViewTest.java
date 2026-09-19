package com.example.uc6;

import com.example.PrintTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.dashboard.Dashboard;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.html.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PrintDashboardView.class)
class PrintDashboardViewTest extends SpringBrowserlessTest {

    @Test
    void reportHasThreeWidgetsThatStayWholeOnPaper() {
        navigate(PrintDashboardView.class);

        Dashboard dashboard = findInView(Dashboard.class).single();
        assertEquals(
                java.util.List.of("Revenue by customer", "Orders per week",
                        "Largest orders"),
                dashboard.getWidgets().stream().map(DashboardWidget::getTitle)
                        .toList());
        assertTrue(dashboard.getWidgets().stream()
                .allMatch(widget -> widget.hasClassName("avoid-break")));
        assertEquals(2, findInView(Chart.class).all().size());
        assertEquals(6, findInView(Table.class).single().getBodyRows().size());
    }

    @Test
    void chartsAreReflowedWhenTheBrowserStartsPrinting() {
        navigate(PrintDashboardView.class);
        // Element-level executeJs is only queued when the response is
        // written, unlike Page#executeJs.
        roundTrip();

        assertTrue(PrintTestSupport.pendingJsContains("beforeprint"),
                "A chart keeps its screen width unless it is told to reflow");
        assertTrue(PrintTestSupport.pendingJsContains("reflow"));
    }

    @Test
    void printButtonOpensThePrintDialog() {
        navigate(PrintDashboardView.class);

        test(findInView(Button.class).id("print-button")).click();

        assertTrue(PrintTestSupport.printRequested());
    }
}
