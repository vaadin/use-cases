package com.example.usecase23;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase23View.class)
@WithMockUser
class UseCase23ViewTest extends SpringBrowserlessTest {

    @SuppressWarnings("deprecation")
    @Test
    void viewRendersWithBoard() {
        navigate(UseCase23View.class);

        assertEquals(1, findInView(Board.class).all().size());
    }

    @Test
    void highlightCardsPresent() {
        navigate(UseCase23View.class);
        runPendingSignalsTasks();

        // 4 highlight cards with H2 titles: "Current users", "View events",
        // "Conversion rate", "Custom metric"
        assertTrue(findInView(H2.class).all().stream()
                .anyMatch(h -> "Current users".equals(h.getText())));
        assertTrue(findInView(H2.class).all().stream()
                .anyMatch(h -> "View events".equals(h.getText())));
    }

    @Test
    void serviceHealthGridRendered() {
        navigate(UseCase23View.class);
        runPendingSignalsTasks();

        // Grid for service health should be present
        assertTrue(findInView(Grid.class).all().size() >= 1);
    }

    @Test
    void highlightCardUpdatesValueOnDataUpdate() {
        navigate(UseCase23View.class);
        runPendingSignalsTasks();

        UseCase23View view = findInView(UseCase23View.class).first();

        // Push data update with known values
        view.onDataUpdate(createTestData(42, 1500, 3.5, 99));
        runPendingSignalsTasks();

        // "Current users" card should show "42"
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> "42".equals(s.getText())),
                "Expected a Span with text '42' for current users");
    }

    @Test
    void highlightCardShowsPercentageChangeAfterTwoUpdates() {
        navigate(UseCase23View.class);
        runPendingSignalsTasks();

        UseCase23View view = findInView(UseCase23View.class).first();

        // First update: baseline
        view.onDataUpdate(createTestData(100, 1000, 2.0, 50));
        runPendingSignalsTasks();

        // Second update: double the users (100 → 200 = +100%)
        view.onDataUpdate(createTestData(200, 1000, 2.0, 50));
        runPendingSignalsTasks();

        // Should show "+100.0" in the percentage badge
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains("+100.0")),
                "Expected percentage badge showing +100.0");
    }

    private DashboardData createTestData(int users, int views,
            double conversion, double custom) {
        return new DashboardData(users, views, conversion, custom,
                new DashboardData.TimelineData("12:00", 10, 20, 30, 40),
                List.of(new ServiceHealth(ServiceHealth.Status.OK, "Münster",
                        100, 200),
                        new ServiceHealth(ServiceHealth.Status.EXCELLENT,
                                "Cluj-Napoca", 150, 250),
                        new ServiceHealth(ServiceHealth.Status.FAILING,
                                "Ciudad Victoria", 50, 75)),
                List.of(10.0, 20.0, 30.0, 15.0, 25.0, 35.0));
    }
}
