package com.example.uc8;

import java.util.List;

import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks the counters for everything browserless tests can simulate: navigation
 * within a UI and a new session. Reloads and tab duplication cannot be
 * simulated yet; see API-GAPS.md.
 */
@SpringBootTest
@ViewPackages(packages = "com.example")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ScopePlaygroundViewTest extends SpringBrowserlessTest {

    @Test
    void firstVisitCountsOneInEveryScope() {
        navigate(ScopePlaygroundView.class);

        assertEquals("UC8 — Scope playground",
                findInView(H1.class).single().getText());
        assertEquals(List.of(1, 1, 1, 1, 1, 1), visits());
    }

    @Test
    void countersShowTheLifetimeOfEachScope() {
        navigate(ScopePlaygroundView.class);

        // Navigating away and back in the same UI: only the route scope
        // starts over.
        navigate(HomeView.class);
        navigate(ScopePlaygroundView.class);
        assertEquals(List.of(2, 2, 2, 2, 2, 1), visits());

        // A new Vaadin session: everything except the application scope
        // starts over. The Spring HTTP session keeps counting only because
        // browserless tests do not give Spring a new HTTP session here; see
        // API-GAPS.md. In a real browser it is 1 as well.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(ScopePlaygroundView.class);
        assertEquals(List.of(3, 3, 1, 1, 1, 1), visits());
    }

    /**
     * Visit counts in display order: application, HTTP session, Vaadin session,
     * browser tab, UI, route.
     */
    private List<Integer> visits() {
        return findInView(Span.class).withClassName("scope-badge").all()
                .stream().map(s -> Integer.parseInt(s.getText().split(" ")[0]))
                .toList();
    }
}
