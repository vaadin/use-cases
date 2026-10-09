package com.example.uc1;

import java.util.List;

import com.example.ManualLatency;
import com.example.backend.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MillionRowGridView.class)
class MillionRowGridViewTest extends SpringBrowserlessTest {

    @Autowired
    private ManualLatency latency;

    @BeforeEach
    void resetLatency() {
        latency.reset();
    }

    @Test
    void gridShowsTheFirstPageWithoutCounting() {
        MillionRowGridView view = navigate(MillionRowGridView.class);

        assertEquals("UC1 — Browse a million rows",
                findInView(H1.class).single().getText());
        List<Order> firstRows = firstRows(view, 3);
        assertEquals(List.of(1L, 2L, 3L),
                firstRows.stream().map(Order::id).toList());
        assertTrue(statsText().endsWith("0 count queries"),
                "the estimate should spare the count query: " + statsText());
        assertTrue(!latency.blocked().isEmpty(),
                "each page fetch is a blocking backend call");
    }

    @Test
    void sortingAndFilteringHappenInTheBackend() {
        MillionRowGridView view = navigate(MillionRowGridView.class);

        view.sortNewestFirst();
        assertEquals(1_000_000L, firstRows(view, 1).getFirst().id());

        test(findInView(TextField.class).single()).setValue("bakery");
        assertTrue(firstRows(view, 20).stream()
                .allMatch(order -> order.customer().equals("Aurora Bakery")));
    }

    @Test
    void exactCountAsksTheBackendToCount() {
        navigate(MillionRowGridView.class);

        test(findInView(Checkbox.class).single()).click();
        test(findInView(TextField.class).single()).setValue("bakery");
        roundTrip();

        assertTrue(!statsText().endsWith(" 0 count queries"),
                "an exact count needs count queries: " + statsText());
    }

    private static List<Order> firstRows(MillionRowGridView view, int count) {
        return view.grid().getLazyDataView().getItems().limit(count).toList();
    }

    private String statsText() {
        return findInView(Span.class).all().stream()
                .filter(span -> span.getClassNames().contains("fetch-stats"))
                .findFirst().orElseThrow().getText();
    }
}
