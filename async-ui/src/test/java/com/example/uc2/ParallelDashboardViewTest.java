package com.example.uc2;

import java.util.List;

import com.example.AsyncState;
import com.example.ManualLatency;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ParallelDashboardView.class)
class ParallelDashboardViewTest extends SpringBrowserlessTest {

    @Autowired
    private ManualLatency latency;

    @BeforeEach
    void resetLatency() {
        latency.reset();
    }

    @Test
    void allWidgetsStartLoadingTogether() {
        ParallelDashboardView view = navigate(ParallelDashboardView.class);

        assertEquals("UC2 — Parallel dashboard",
                findInView(H1.class).single().getText());
        assertEquals(4, latency.pending().size(),
                "every widget should query at once, not one after another");
        assertTrue(view.widgets().stream()
                .allMatch(widget -> widget.state().isLoading()));
    }

    @Test
    void eachWidgetFillsInWhenItsOwnQueryAnswers() {
        ParallelDashboardView view = navigate(ParallelDashboardView.class);

        // The third widget answers first.
        latency.complete(2);
        runPendingSignalsTasks();

        assertEquals(new AsyncState.Loaded<>("Espresso beans 1 kg"),
                view.widgets().get(2).state());
        assertTrue(view.widgets().get(0).state().isLoading());
        assertTrue(spanTexts().contains("Espresso beans 1 kg"));

        latency.completePending();
        runPendingSignalsTasks();

        assertTrue(view.widgets().stream()
                .allMatch(widget -> widget.state().isLoaded()));
        assertTrue(spanTexts().contains("€ 18,240"));
    }

    @Test
    void failingWidgetShowsErrorAndRetries() {
        ParallelDashboardView view = navigate(ParallelDashboardView.class);
        test(find(Checkbox.class).single()).click();
        test(button("Reload all")).click();
        latency.completePending();
        runPendingSignalsTasks();

        ParallelDashboardView.Widget backlog = view.widgets().get(3);
        assertTrue(backlog.state().isFailed());
        assertTrue(spanTexts().contains("Ticketing system timed out"));
        // The other widgets are not affected by the failure.
        assertTrue(view.widgets().get(0).state().isLoaded());

        test(find(Checkbox.class).single()).click();
        test(button("Retry")).click();
        assertTrue(backlog.state().isLoading());
        latency.completePending();
        runPendingSignalsTasks();

        assertEquals(new AsyncState.Loaded<>("27 tickets"), backlog.state());
    }

    @Test
    void reloadingDropsTheOlderAnswer() {
        ParallelDashboardView view = navigate(ParallelDashboardView.class);
        test(find(Checkbox.class).single()).click();
        test(button("Reload all")).click();

        // The first round answers after the second one was started: its
        // results must not be shown.
        latency.complete(3);
        runPendingSignalsTasks();
        assertTrue(view.widgets().get(3).state().isLoading(),
                "a superseded answer must not overwrite the newer load");
    }

    private Button button(String text) {
        return find(Button.class).all().stream()
                .filter(button -> text.equals(button.getText())
                        && button.isVisible()
                        && button.getParent().orElseThrow().isVisible())
                .findFirst().orElseThrow();
    }

    private List<String> spanTexts() {
        return find(Span.class).all().stream().filter(Span::isVisible)
                .map(Span::getText).toList();
    }
}
