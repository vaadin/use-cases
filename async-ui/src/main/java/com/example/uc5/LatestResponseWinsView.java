package com.example.uc5;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.MissingAPI;
import com.example.backend.ProductCatalog;
import com.example.backend.SimulatedLatency;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC5 — Latest response wins.
 * <p>
 * Search-as-you-type sends a request per pause in typing, and nothing
 * guarantees the answers come back in the order the requests went out: here a
 * short search term matches more products and takes longer, so "la" may answer
 * after "lap". Without care, the late answer for "la" overwrites the results
 * for "lap" and the list no longer matches the search field.
 * <p>
 * Each request gets a sequence number and only the answer to the newest one is
 * shown; older answers are logged and dropped. Cancelling the older future
 * would not be enough: it does not stop the query the backend has already
 * started, and an answer can be on its way back already. Unchecking the box
 * shows the naive behaviour for comparison. Flow has no "latest wins" helper
 * for asynchronous results (see API-GAPS.md).
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Latest response wins")
@UseCaseDescription("Keeping a late, outdated answer from overwriting a newer one")
@Menu(order = 5, title = "UC5 — Latest response wins")
@StyleSheet("uc5.css")
public class LatestResponseWinsView extends VerticalLayout {

    static final int MAX_LOG_LINES = 8;

    private final ProductCatalog catalog;
    private final SimulatedLatency latency;

    private final TextField search = new TextField("Search products");
    private final Checkbox discardStale = new Checkbox(
            "Discard outdated answers", true);
    private final Span showing = new Span();
    private final UnorderedList results = new UnorderedList();
    private final Div log = new Div();

    private long latestRequest;

    public LatestResponseWinsView(ProductCatalog catalog,
            SimulatedLatency latency) {
        this.catalog = catalog;
        this.latency = latency;
        addClassName("uc5-view");

        add(new H1("UC5 — Latest response wins"));
        add(new Paragraph("Type \"lap\" quickly, one letter at a time. "
                + "Shorter terms are slower to answer, so the answer for "
                + "\"la\" arrives after the one for \"lap\". With outdated "
                + "answers discarded the list always matches the field; "
                + "uncheck the box to see the list jump back to \"la\"."));

        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setValueChangeTimeout(150);
        search.setClearButtonVisible(true);
        search.addValueChangeListener(event -> search(event.getValue()));

        showing.addClassName("showing");
        results.addClassName("results");
        log.addClassName("request-log");

        add(new HorizontalLayout(search, discardStale));
        add(showing, results, new H2("Answers as they arrive"), log);
        showing.setText("Type to search");
    }

    /**
     * Shorter terms match more products and take longer: 2 s for one letter,
     * 0.3 s from four letters on.
     */
    static Duration latencyFor(String text) {
        return Duration.ofMillis(Math.max(300, 2300 - 600L * text.length()));
    }

    private void search(String text) {
        long request = ++latestRequest;
        UI ui = UI.getCurrent();
        CompletableFuture<List<String>> answer = latency.after(latencyFor(text),
                () -> catalog.search(text));
        answer.whenComplete((found, error) -> ui
                .accessLater(() -> deliver(request, text, found, error), null)
                .run());
    }

    private void deliver(long request, String text,
            @Nullable List<String> found, @Nullable Throwable error) {
        if (error != null) {
            logLine("\"" + text + "\" failed: "
                    + MissingAPI.unwrap(error).getMessage(), "stale");
            return;
        }
        if (request != latestRequest && discardStale.getValue()) {
            logLine("\"" + text + "\" answered late — discarded", "stale");
            return;
        }
        logLine("\"" + text + "\" answered — shown", "shown");
        showing.setText("Results for \"" + text + "\"");
        showing.setClassName("mismatch", !text.equals(search.getValue()));
        results.removeAll();
        (found == null ? List.<String> of() : found)
                .forEach(name -> results.add(new ListItem(name)));
    }

    private void logLine(String text, String className) {
        Span line = new Span(text);
        line.addClassName(className);
        log.addComponentAsFirst(line);
        while (log.getComponentCount() > MAX_LOG_LINES) {
            log.remove(log.getComponentAt(MAX_LOG_LINES));
        }
    }
}
