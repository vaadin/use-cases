package com.example.uc11;

import java.time.Duration;
import java.util.List;

import com.example.AsyncState;
import com.example.MissingAPI;
import com.example.backend.Order;
import com.example.backend.OrderService;
import com.example.backend.SimulatedLatency;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC11 — Grid with a search that shows it is loading.
 * <p>
 * Searching the order book takes the backend over a second. The search runs in
 * the background, so the field stays responsive, and while it runs the UI says
 * so: a spinner in the search field, a progress bar over the Grid, and the
 * previous results kept but dimmed rather than cleared, so the page does not
 * jump. On the very first load there are no previous results, so the Grid shows
 * skeleton rows instead. A search without matches gets its own empty state, and
 * a failed search an error with a retry button. Typing on while a search is
 * running replaces it; an older answer is never shown.
 * <p>
 * The Grid has no loading state the server can set (see API-GAPS.md), so the
 * view builds one from a CSS class, a progress bar and the empty-state
 * component.
 */
@Route(value = "uc11", layout = MainLayout.class)
@PageTitle("UC11 — Grid search with loading state")
@UseCaseDescription("Showing that a slow Grid search is running, has no results or failed")
@Menu(order = 11, title = "UC11 — Grid search loading")
@StyleSheet("uc11.css")
public class GridSearchLoadingView extends VerticalLayout {

    static final Duration SEARCH_LATENCY = Duration.ofMillis(1500);
    static final int MAX_RESULTS = 500;

    private final OrderService orders;
    private final SimulatedLatency latency;

    private final TextField search = new TextField();
    private final Checkbox failSearch = new Checkbox("Make the search fail");
    private final Grid<Order> grid = new Grid<>();
    private final ValueSignal<AsyncState<List<Order>>> results = new ValueSignal<>(
            AsyncState.loading());
    private final ValueSignal<String> searchedFor = new ValueSignal<>("");
    private final Div skeletonRows = new Div();
    private final Span noMatches = new Span();
    private @Nullable Registration pending;

    public GridSearchLoadingView(OrderService orders,
            SimulatedLatency latency) {
        this.orders = orders;
        this.latency = latency;
        addClassName("uc11-view");
        setSizeFull();

        add(new H1("UC11 — Grid search with loading state"));
        add(new Paragraph("Search the order book by customer, e.g. "
                + "\"bakery\" or \"café\". Each search takes 1.5 seconds: "
                + "watch the spinner in the field, the bar over the Grid and "
                + "the dimmed previous results. Try \"zzz\" for no matches, "
                + "or make the search fail."));

        Signal<Boolean> loading = results.map(AsyncState::isLoading);

        Span spinner = new Span();
        spinner.addClassName("spinner");
        spinner.bindVisible(loading);
        search.setPlaceholder("Search customers");
        search.setClearButtonVisible(true);
        search.setSuffixComponent(spinner);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(event -> search());
        add(new HorizontalLayout(search, failSearch));

        Span summary = new Span();
        summary.addClassName("search-summary");
        summary.bindText(() -> summary(results.get(), searchedFor.get()));
        Button retry = new Button("Retry", event -> search());
        retry.addThemeVariants(ButtonVariant.SMALL);
        retry.bindVisible(results.map(AsyncState::isFailed));
        add(new HorizontalLayout(summary, retry));

        ProgressBar progress = new ProgressBar();
        progress.setIndeterminate(true);
        progress.addClassName("grid-progress");
        progress.bindVisible(loading);

        grid.addColumn(Order::id).setHeader("Order").setAutoWidth(true);
        grid.addColumn(Order::customer).setHeader("Customer");
        grid.addColumn(Order::product).setHeader("Product");
        grid.addColumn(Order::total).setHeader("Total").setAutoWidth(true);
        grid.addColumn(Order::ordered).setHeader("Ordered").setAutoWidth(true);
        grid.setSizeFull();
        grid.bindClassName("loading", loading);
        Signal.effect(grid, () -> show(results.get()));

        skeletonRows.addClassName("skeleton-rows");
        for (int i = 0; i < 8; i++) {
            Div row = new Div();
            row.addClassName("skeleton");
            skeletonRows.add(row);
        }
        noMatches.addClassName("no-matches");

        Div gridWrapper = new Div(progress, grid);
        gridWrapper.addClassName("grid-wrapper");
        gridWrapper.setWidthFull();
        add(gridWrapper);
        // Take the remaining height; at 100% it would squash the search row.
        setFlexGrow(1, gridWrapper);

        search();
    }

    private void search() {
        if (pending != null) {
            pending.remove();
        }
        String text = search.getValue();
        boolean fail = failSearch.getValue();
        searchedFor.set(text);
        pending = MissingAPI.load(this, results,
                () -> latency.after(SEARCH_LATENCY, () -> {
                    if (fail) {
                        throw new IllegalStateException(
                                "The search service is not responding");
                    }
                    return orders.fetch(text, false, 0, MAX_RESULTS).toList();
                }));
    }

    /**
     * Keeps the previous rows while a new search runs, and picks the empty
     * state that fits: skeleton rows before the first answer, a message when
     * nothing matched.
     */
    private void show(AsyncState<List<Order>> state) {
        switch (state) {
        case AsyncState.Loading<List<Order>> loading -> {
            if (grid.getListDataView().getItemCount() == 0) {
                grid.setEmptyStateComponent(skeletonRows);
            }
        }
        case AsyncState.Loaded<List<Order>>(List<Order> rows) -> {
            noMatches.setText("No orders for \"" + searchedFor.peek() + "\".");
            grid.setEmptyStateComponent(noMatches);
            grid.setItems(rows);
        }
        case AsyncState.Failed<List<Order>> failed -> {
            grid.setEmptyStateComponent(null);
            grid.setItems(List.of());
        }
        }
    }

    private static String summary(AsyncState<List<Order>> state, String text) {
        String target = text.isBlank() ? "all orders" : "\"" + text + "\"";
        return switch (state) {
        case AsyncState.Loading<List<Order>> loading ->
            "Searching " + target + "…";
        case AsyncState.Loaded<List<Order>>(List<Order> rows) ->
            rows.size() == MAX_RESULTS
                    ? "First %d matches for %s".formatted(MAX_RESULTS, target)
                    : "%d matches for %s".formatted(rows.size(), target);
        case AsyncState.Failed<List<Order>>(Throwable error) ->
            "Search for " + target + " failed: " + error.getMessage();
        };
    }

    // Package-private test seams.
    Grid<Order> grid() {
        return grid;
    }

    AsyncState<List<Order>> results() {
        return results.peek();
    }
}
