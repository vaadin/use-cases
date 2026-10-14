package com.example.uc1;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import com.example.backend.Order;
import com.example.backend.OrderService;
import com.example.backend.SimulatedLatency;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Browse a million rows.
 * <p>
 * The Grid never holds the order book: it asks the backend for one page at a
 * time while the user scrolls, sorted and filtered by the backend. Asking for
 * the exact number of rows is the expensive part — with a filter it is a scan
 * of the whole table — so by default the Grid works from an <em>estimate</em>
 * and grows the scrollbar as the user nears the end. The checkbox switches to
 * an exact count so the difference shows in the query log.
 * <p>
 * Every fetch is a blocking call made on the request thread: a data provider
 * cannot answer asynchronously (see API-GAPS.md).
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Browse a million rows")
@UseCaseDescription("Paging, sorting and filtering a large table in the backend")
@Menu(order = 1, title = "UC1 — Browse a million rows")
@StyleSheet("uc1.css")
public class MillionRowGridView extends VerticalLayout {

    static final Duration FETCH_LATENCY = Duration.ofMillis(80);
    static final int ESTIMATE = 1_000;

    private final OrderService orders;
    private final SimulatedLatency latency;

    private final Grid<Order> grid = new Grid<>(Order.class, false);
    private final TextField filter = new TextField("Customer");
    private final Checkbox exactCount = new Checkbox(
            "Ask the backend for the exact row count");
    private final Span fetchStats = new Span();
    private final Span lastQuery = new Span();

    private int fetches;
    private int counts;

    public MillionRowGridView(OrderService orders, SimulatedLatency latency) {
        this.orders = orders;
        this.latency = latency;
        addClassName("uc1-view");
        setSizeFull();

        add(new H1("UC1 — Browse a million rows"));
        add(new Paragraph("The order book below has "
                + "%,d rows. The Grid fetches one page at a time as you "
                        .formatted(OrderService.ORDER_COUNT)
                + "scroll, and the backend does the sorting (click the Order "
                + "column) and the filtering. Without an exact count the "
                + "scrollbar starts from an estimate and grows as you scroll "
                + "towards the end."));

        filter.setPlaceholder("e.g. bakery");
        filter.setClearButtonVisible(true);
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.addValueChangeListener(event -> bindItems());
        exactCount.addValueChangeListener(event -> bindItems());

        grid.addColumn(Order::id).setHeader("Order").setKey("id")
                .setSortable(true).setAutoWidth(true);
        grid.addColumn(Order::customer).setHeader("Customer");
        grid.addColumn(Order::product).setHeader("Product");
        grid.addColumn(Order::quantity).setHeader("Qty").setAutoWidth(true);
        grid.addColumn(Order::total).setHeader("Total").setAutoWidth(true);
        grid.addColumn(Order::ordered).setHeader("Ordered").setAutoWidth(true);
        grid.setPageSize(50);
        grid.setSizeFull();

        fetchStats.addClassName("fetch-stats");
        lastQuery.addClassName("last-query");
        HorizontalLayout controls = new HorizontalLayout(filter, exactCount);
        controls.setAlignItems(Alignment.BASELINE);
        add(controls, grid, new HorizontalLayout(fetchStats, lastQuery));

        bindItems();
    }

    private void bindItems() {
        String text = filter.getValue();
        if (exactCount.getValue()) {
            grid.setItems(query -> fetch(text, query), query -> {
                counts++;
                latency.block(FETCH_LATENCY);
                int count = orders.count(text);
                renderStats();
                return count;
            });
        } else {
            grid.setItems(query -> fetch(text, query))
                    .setItemCountEstimate(ESTIMATE);
        }
        renderStats();
    }

    private Stream<Order> fetch(String text, Query<Order, Void> query) {
        fetches++;
        latency.block(FETCH_LATENCY);
        boolean descending = query.getSortOrders().stream()
                .anyMatch(order -> order.getSorted().equals("id")
                        && order.getDirection() == SortDirection.DESCENDING);
        List<Order> page = orders
                .fetch(text, descending, query.getOffset(), query.getLimit())
                .toList();
        lastQuery.setText("Last page: rows %,d–%,d%s".formatted(
                query.getOffset() + 1, query.getOffset() + page.size(),
                descending ? ", newest first" : ""));
        renderStats();
        return page.stream();
    }

    private void renderStats() {
        fetchStats.setText(
                "%d page fetches, %d count queries".formatted(fetches, counts));
    }

    // Package-private test seams.
    Grid<Order> grid() {
        return grid;
    }

    void sortNewestFirst() {
        grid.sort(GridSortOrder.desc(grid.getColumnByKey("id")).build());
    }
}
