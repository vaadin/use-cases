package com.example.uc4;

import com.example.PageVisibilityTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PageVisibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = RefreshStaleDataView.class)
class RefreshStaleDataViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithRateAndManualRefresh() {
        navigate(RefreshStaleDataView.class);
        runPendingSignalsTasks();

        // The card shows a 4-decimal exchange rate.
        assertTrue(findInView(Span.class).all().stream().anyMatch(s -> {
            String t = s.getText();
            return t != null && t.matches("\\d+\\.\\d{4}");
        }), "rate value should be rendered as 0.0000-style number");

        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Refresh now".equals(b.getText())));
    }

    @Test
    void manualRefreshUpdatesRateValue() {
        navigate(RefreshStaleDataView.class);
        runPendingSignalsTasks();

        Span before = findInView(Span.class).all().stream()
                .filter(s -> s.getText() != null
                        && s.getText().matches("\\d+\\.\\d{4}"))
                .findFirst().orElseThrow();
        String beforeRate = before.getText();

        // Click refresh enough times to almost certainly land on a different
        // value (the random walk has a 50% chance per click of changing
        // direction, but the value differs from the previous one with very
        // high probability after a few clicks).
        Button refresh = findInView(Button.class).all().stream()
                .filter(b -> "Refresh now".equals(b.getText())).findFirst()
                .orElseThrow();
        for (int i = 0; i < 10; i++) {
            test(refresh).click();
            runPendingSignalsTasks();
        }

        Span after = findInView(Span.class).all().stream()
                .filter(s -> s.getText() != null
                        && s.getText().matches("\\d+\\.\\d{4}"))
                .findFirst().orElseThrow();
        assertNotEquals(beforeRate, after.getText(),
                "rate should change after several manual refreshes");
    }

    @Test
    void hiddenForLongerThanThresholdRefreshesOnReturn() {
        RefreshStaleDataView view = navigate(RefreshStaleDataView.class);
        runPendingSignalsTasks();
        double before = view.currentRate();

        PageVisibilityTestSupport.setPageVisibility(PageVisibility.HIDDEN);
        runPendingSignalsTasks();
        view.backdateHiddenAt(10);

        PageVisibilityTestSupport.setPageVisibility(PageVisibility.VISIBLE);
        runPendingSignalsTasks();

        assertNotEquals(before, view.currentRate(),
                "rate should auto-refresh after returning from a long hide");
    }

    @Test
    void shortHideDoesNotRefreshOnReturn() {
        RefreshStaleDataView view = navigate(RefreshStaleDataView.class);
        runPendingSignalsTasks();
        double before = view.currentRate();

        PageVisibilityTestSupport.setPageVisibility(PageVisibility.HIDDEN);
        runPendingSignalsTasks();
        // Leave hiddenAt at "just now" — far below the 5-second threshold.
        PageVisibilityTestSupport.setPageVisibility(PageVisibility.VISIBLE);
        runPendingSignalsTasks();

        assertEquals(before, view.currentRate(),
                "quick alt-tab should not trigger a refresh");
    }
}
