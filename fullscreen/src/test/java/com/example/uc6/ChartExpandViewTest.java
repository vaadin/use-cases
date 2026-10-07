package com.example.uc6;

import com.example.FullscreenTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ChartExpandView.class)
class ChartExpandViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithThreeChartCardsAndExpandButtons() {
        navigate(ChartExpandView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC6 — Chart expand-to-fullscreen".equals(h.getText())));

        long chartCardCount = findInView(Div.class).all().stream()
                .filter(d -> d.getClassNames().contains("chart-card")).count();
        assertEquals(3, chartCardCount, "expected three chart cards");

        long expandButtons = findInView(Button.class).all().stream()
                .filter(b -> "Expand".equals(b.getText())).count();
        assertEquals(3, expandButtons, "expected one Expand button per card");
    }

    @Test
    void expandingASingleCardSetsExpandedClassOnThatCardOnly() {
        navigate(ChartExpandView.class);
        runPendingSignalsTasks();

        // Before any click, no card carries the expanded class.
        assertExpandedTitles();

        // Click the Expand button next to the "Conversion" card. The middle
        // Expand button corresponds to it (cards are added in CHART_TITLES
        // order: Visitors, Conversion, Revenue).
        Button conversionExpand = findInView(Button.class).all().stream()
                .filter(b -> "Expand".equals(b.getText())).skip(1).findFirst()
                .orElseThrow();
        test(conversionExpand).click();
        runPendingSignalsTasks();

        assertExpandedTitles("Conversion");
    }

    @Test
    void sessionEndExitedByUserClearsExpandedClass() {
        navigate(ChartExpandView.class);
        runPendingSignalsTasks();

        Button visitorsExpand = findInView(Button.class).all().stream()
                .filter(b -> "Expand".equals(b.getText())).findFirst()
                .orElseThrow();
        test(visitorsExpand).click();
        runPendingSignalsTasks();
        assertExpandedTitles("Visitors");

        // Drive the global signal through FULLSCREEN so the simulator marks
        // the open session as EXITED_BY_USER on the way back out.
        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();

        assertExpandedTitles();
    }

    /**
     * Asserts that the cards whose first header span matches one of the given
     * titles carry the {@code expanded} class, and no other cards do.
     */
    private void assertExpandedTitles(String... titles) {
        java.util.Set<String> expected = java.util.Set.of(titles);
        java.util.Set<String> actual = new java.util.LinkedHashSet<>();
        for (Div card : findInView(Div.class).all().stream()
                .filter(d -> d.getClassNames().contains("chart-card"))
                .toList()) {
            if (card.getClassNames().contains("expanded")) {
                card.getChildren().flatMap(c -> c.getChildren())
                        .filter(c -> c instanceof Span)
                        .map(c -> ((Span) c).getText()).findFirst()
                        .ifPresent(actual::add);
            }
        }
        assertEquals(expected, actual,
                "wrong set of cards carrying the expanded class");
    }
}
