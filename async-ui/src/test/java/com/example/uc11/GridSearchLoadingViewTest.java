package com.example.uc11;

import java.util.List;

import com.example.ManualLatency;
import com.example.backend.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = GridSearchLoadingView.class)
class GridSearchLoadingViewTest extends SpringBrowserlessTest {

    @Autowired
    private ManualLatency latency;

    @BeforeEach
    void resetLatency() {
        latency.reset();
    }

    @Test
    void firstLoadShowsSkeletonRowsUntilTheAnswerArrives() {
        GridSearchLoadingView view = navigate(GridSearchLoadingView.class);
        runPendingSignalsTasks();

        assertEquals("UC11 — Grid search with loading state",
                findInView(H1.class).single().getText());
        assertLoading(view, true);
        assertTrue(view.grid().getEmptyStateComponent().getClassNames()
                .contains("skeleton-rows"));
        assertEquals("Searching all orders…", summary());

        completeSearch();

        assertLoading(view, false);
        assertEquals(GridSearchLoadingView.MAX_RESULTS, rowCount(view));
        assertEquals("First 500 matches for all orders", summary());
    }

    @Test
    void previousResultsStayWhileANewSearchRuns() {
        GridSearchLoadingView view = navigate(GridSearchLoadingView.class);
        completeSearch();

        test(findInView(TextField.class).single()).setValue("bakery");
        runPendingSignalsTasks();

        assertLoading(view, true);
        assertEquals(GridSearchLoadingView.MAX_RESULTS, rowCount(view),
                "the old rows stay, dimmed, instead of being cleared");
        assertEquals("Searching \"bakery\"…", summary());

        completeSearch();
        assertTrue(rows(view).stream()
                .allMatch(order -> order.customer().equals("Aurora Bakery")));
    }

    @Test
    void searchWithoutMatchesShowsItsOwnEmptyState() {
        GridSearchLoadingView view = navigate(GridSearchLoadingView.class);
        completeSearch();

        test(findInView(TextField.class).single()).setValue("zzz");
        completeSearch();

        assertEquals(0, rowCount(view));
        assertEquals("No orders for \"zzz\".",
                ((Span) view.grid().getEmptyStateComponent()).getText());
        assertEquals("0 matches for \"zzz\"", summary());
    }

    @Test
    void failedSearchOffersRetry() {
        GridSearchLoadingView view = navigate(GridSearchLoadingView.class);
        test(findInView(Checkbox.class).single()).click();
        test(findInView(TextField.class).single()).setValue("bakery");
        latency.completePending();
        runPendingSignalsTasks();

        assertTrue(view.results().isFailed());
        assertEquals("Search for \"bakery\" failed: The search service is "
                + "not responding", summary());
        Button retry = findInView(Button.class).single();

        test(findInView(Checkbox.class).single()).click();
        test(retry).click();
        completeSearch();
        assertTrue(view.results().isLoaded());
        assertFalse(retry.isVisible());
    }

    @Test
    void olderSearchAnsweringLastIsNotShown() {
        GridSearchLoadingView view = navigate(GridSearchLoadingView.class);
        TextField field = findInView(TextField.class).single();
        test(field).setValue("café");
        test(field).setValue("bakery");

        // Answers arrive in reverse order: "bakery", then "café", then the
        // initial load.
        for (int i = latency.pending().size() - 1; i >= 0; i--) {
            latency.complete(i);
            runPendingSignalsTasks();
        }

        assertEquals("First 500 matches for \"bakery\"", summary());
        assertTrue(rows(view).stream()
                .allMatch(order -> order.customer().equals("Aurora Bakery")));
    }

    private void completeSearch() {
        latency.completePending();
        runPendingSignalsTasks();
    }

    private void assertLoading(GridSearchLoadingView view, boolean loading) {
        assertEquals(loading, view.grid().getClassNames().contains("loading"));
        // Queries only see visible components: the bar is found only while
        // the search runs.
        assertEquals(loading ? 1 : 0,
                findInView(ProgressBar.class).all().size());
    }

    private static int rowCount(GridSearchLoadingView view) {
        return view.grid().getListDataView().getItemCount();
    }

    private static List<Order> rows(GridSearchLoadingView view) {
        return view.grid().getListDataView().getItems().toList();
    }

    private String summary() {
        return findInView(Span.class).all().stream()
                .filter(span -> span.getClassNames().contains("search-summary"))
                .findFirst().orElseThrow().getText();
    }
}
