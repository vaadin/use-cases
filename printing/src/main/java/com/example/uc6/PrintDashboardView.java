package com.example.uc6;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.MissingAPI;
import com.example.data.Order;
import com.example.data.Orders;
import com.example.print.ChartPrintReflow;
import com.example.print.OrderDocument;
import com.example.print.PrintColumn;
import com.example.print.PrintColumns;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.dashboard.Dashboard;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC6 — Printing a dashboard of charts.
 * <p>
 * A management report is a dashboard on paper, and a dashboard is the hardest
 * thing in this module to print. Three separate problems meet here.
 * <ul>
 * <li>A chart sizes its SVG once, in pixels, when it is drawn. Print CSS
 * changes the layout width underneath it and nothing tells the chart to
 * re-measure, so it prints clipped or overflowing. The fix is a
 * {@code beforeprint} listener that reflows every chart — client-side, because
 * printing is synchronous and the server cannot answer in time.</li>
 * <li>A dashboard lays itself out in columns sized for a screen. On A4 those
 * columns are too narrow to read, so print CSS collapses the dashboard to a
 * single column and forbids breaking a widget across sheets.</li>
 * <li>Both the dashboard and the chart keep their layout inside a shadow root,
 * so every one of those rules has to go through {@code ::part()} — and only for
 * the parts that happen to be exposed.</li>
 * </ul>
 *
 * @see <a href="https://github.com/vaadin/charts/issues/530">vaadin/charts#530
 *      — Print Chart breaks SplitLayout in Chrome</a>
 * @see <a href="https://github.com/vaadin/board/issues/103">vaadin/board#103 —
 *      PDF/Print Request</a>
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Printing a dashboard")
@Menu(order = 6, title = "UC6 — Printing a dashboard")
@StyleSheet("uc6.css")
public class PrintDashboardView extends VerticalLayout {

    private static final int ORDER_COUNT = 24;

    private static final List<PrintColumn<Order>> TOP_COLUMNS = List.of(
            PrintColumn.of("Order", Order::id),
            PrintColumn.of("Customer", Order::customer), PrintColumn.numeric(
                    "Total", order -> OrderDocument.money(order.total())));

    public PrintDashboardView() {
        Div intro = new Div();
        intro.addClassName("no-print");
        intro.add(new H1("UC6 — Printing a dashboard"));
        intro.add(new Paragraph(
                "Three widgets of the same order data. Printing reflows the "
                        + "charts to the paper width, stacks the widgets into "
                        + "one column and keeps each of them whole on a "
                        + "single sheet."));

        Button print = new Button("Print the report", event -> MissingAPI
                .print(event.getSource().getUI().orElseThrow()));
        print.addThemeVariants(ButtonVariant.PRIMARY);
        print.addClassName("no-print");
        print.setId("print-button");

        List<Order> orders = Orders.sample(ORDER_COUNT);

        Dashboard dashboard = new Dashboard();
        dashboard.addClassNames("report-dashboard", "printable");
        dashboard.setId("report-dashboard");
        dashboard.setMaximumColumnCount(2);
        dashboard.add(widget("Revenue by customer", revenueChart(orders)),
                widget("Orders per week", volumeChart(orders)),
                widget("Largest orders", topOrders(orders)));

        add(intro, print, dashboard, new ChartPrintReflow(dashboard));
    }

    private static DashboardWidget widget(String title, Component content) {
        DashboardWidget widget = new DashboardWidget(title, content);
        widget.addClassName("avoid-break");
        return widget;
    }

    private static Chart revenueChart(List<Order> orders) {
        Chart chart = new Chart(ChartType.PIE);
        Configuration configuration = chart.getConfiguration();
        configuration.setTitle("");
        DataSeries series = new DataSeries();
        orders.stream()
                .collect(Collectors.groupingBy(Order::customer,
                        Collectors.reducing(BigDecimal.ZERO, Order::total,
                                BigDecimal::add)))
                .entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> series.add(new DataSeriesItem(entry.getKey(),
                        entry.getValue().doubleValue())));
        configuration.addSeries(series);
        return chart;
    }

    private static Chart volumeChart(List<Order> orders) {
        Chart chart = new Chart(ChartType.COLUMN);
        Configuration configuration = chart.getConfiguration();
        configuration.setTitle("");
        int weeks = 4;
        Number[] perWeek = new Number[weeks];
        for (int week = 0; week < weeks; week++) {
            int from = week * orders.size() / weeks;
            int to = (week + 1) * orders.size() / weeks;
            perWeek[week] = orders.subList(from, to).stream()
                    .mapToInt(Order::itemCount).sum();
        }
        configuration.addSeries(new ListSeries("Items", perWeek));
        XAxis xAxis = new XAxis();
        xAxis.setCategories("Week 3", "Week 4", "Week 5", "Week 6");
        configuration.addxAxis(xAxis);
        return chart;
    }

    private static Component topOrders(List<Order> orders) {
        List<Order> top = orders.stream()
                .sorted(Comparator.comparing(Order::total).reversed()).limit(6)
                .toList();
        return PrintColumns.asTable(TOP_COLUMNS, top);
    }
}
