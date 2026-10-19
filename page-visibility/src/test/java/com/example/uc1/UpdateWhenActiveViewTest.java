package com.example.uc1;

import java.time.Duration;

import com.example.PageVisibilityTestSupport;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PageVisibility;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { UpdateWhenActiveView.class, HomeView.class })
class UpdateWhenActiveViewTest extends SpringBrowserlessTest {

    private static final Duration TICK = Duration.ofSeconds(1);

    @Test
    void statusBadgeReflectsPageVisibilityChanges() {
        navigate(UpdateWhenActiveView.class);
        runPendingSignalsTasks();

        // Mock UI starts at UNKNOWN; the binding renders the matching label.
        assertBadgeContains("Visibility unknown");

        setPageVisibility(PageVisibility.VISIBLE);
        assertBadgeContains("Updating");

        setPageVisibility(PageVisibility.HIDDEN);
        assertBadgeContains("tab hidden");

        setPageVisibility(PageVisibility.VISIBLE_NOT_FOCUSED);
        assertBadgeContains("not focused");
    }

    @Test
    void clockTicksWhileVisibleAndPausesWhileHidden() throws Exception {
        UpdateWhenActiveView view = navigate(UpdateWhenActiveView.class);
        setPageVisibility(PageVisibility.VISIBLE);

        await().atMost(TICK.multipliedBy(5)).until(() -> view.updates() > 0);
        runPendingSignalsTasks();
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> String.valueOf(view.updates())
                                .equals(s.getText())),
                "the counter label should show the ticks received");

        setPageVisibility(PageVisibility.HIDDEN);
        assertNoMoreTicks(view);
    }

    @Test
    void leavingTheViewStopsTheClock() throws Exception {
        UpdateWhenActiveView view = navigate(UpdateWhenActiveView.class);
        setPageVisibility(PageVisibility.VISIBLE);
        await().atMost(TICK.multipliedBy(5)).until(() -> view.updates() > 0);

        navigate(HomeView.class);
        assertNoMoreTicks(view);
    }

    private static void assertNoMoreTicks(UpdateWhenActiveView view)
            throws InterruptedException {
        // A tick that was already running may still land.
        Thread.sleep(TICK.toMillis() / 2);
        int updates = view.updates();
        Thread.sleep(TICK.multipliedBy(2).toMillis());
        assertEquals(updates, view.updates(), "no tick should arrive");
    }

    private void assertBadgeContains(String fragment) {
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains(fragment)),
                "expected status badge to contain \"" + fragment + "\"");
    }

    private void setPageVisibility(PageVisibility state) {
        PageVisibilityTestSupport.setPageVisibility(state);
        runPendingSignalsTasks();
    }
}
