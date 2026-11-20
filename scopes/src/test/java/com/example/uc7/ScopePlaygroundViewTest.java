package com.example.uc7;

import java.util.List;
import java.util.stream.IntStream;

import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.ComponentQuery;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks the counters for everything browserless tests can simulate: navigation
 * within a UI, a new tab and another browser. Reloads are not covered yet; tab
 * duplication cannot be simulated; see API-GAPS.md #2.
 */
@SpringBootTest
@ViewPackages(packages = "com.example")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ScopePlaygroundViewTest extends SpringBrowserlessTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void firstVisitCountsOneInEveryScope() {
        navigate(ScopePlaygroundView.class);

        assertEquals("UC7 — Scope playground",
                findInView(H1.class).single().getText());
        assertEquals(List.of(1, 1, 1, 1, 1, 1), visits(findInView(Span.class)));
        assertEquals(List.of(true, true, true, true, true, true),
                startedOver(findInView(Span.class)));
    }

    @Test
    void countersShowTheLifetimeOfEachScope() {
        try (var app = SpringBrowserlessApplicationContext
                .create(applicationContext,
                        b -> b.withViewPackages(ScopePlaygroundView.class,
                                HomeView.class)
                                .withSessionLockIsolation(true))) {
            var alice = app.newUser().newWindow();
            alice.navigate(ScopePlaygroundView.class);
            assertEquals(List.of(1, 1, 1, 1, 1, 1),
                    visits(alice.find(Span.class)));

            // Navigating away and back in the same UI: only the route scope
            // starts over.
            alice.navigate(HomeView.class);
            alice.navigate(ScopePlaygroundView.class);
            assertEquals(List.of(2, 2, 2, 2, 2, 1),
                    visits(alice.find(Span.class)));
            assertEquals(List.of(false, false, false, false, false, true),
                    startedOver(alice.find(Span.class)));
            // The "Away and back" column agrees with the counts.
            assertEquals(List.of('✓', '✓', '✓', '✓', '✓', '✗'),
                    marks(alice).stream().map(row -> row.charAt(4)).toList());

            // A new tab of the same session: the browser tab, the UI and the
            // route start over. The UI count is left out: browserless-test
            // gives every UI the id 1, so the @UIScope bean of the first tab
            // is reused (it would be 1 in a browser).
            var aliceNewTab = alice.getUser().newWindow();
            aliceNewTab.navigate(ScopePlaygroundView.class);
            List<Integer> newTabVisits = visits(aliceNewTab.find(Span.class));
            assertEquals(List.of(3, 3, 3, 1), newTabVisits.subList(0, 4));
            assertEquals(1, newTabVisits.get(5));
            assertEquals(List.of('✓', '✓', '✓', '✗', '✗', '✗'),
                    marks(aliceNewTab).stream().map(row -> row.charAt(1))
                            .toList());

            // Another browser: everything except the application scope starts
            // over, the Spring HTTP session as well.
            var bob = app.newUser().newWindow();
            bob.navigate(ScopePlaygroundView.class);
            assertEquals(List.of(4, 1, 1, 1, 1, 1),
                    visits(bob.find(Span.class)));
            assertEquals(List.of('✓', '✗', '✗', '✗', '✗', '✗'),
                    marks(bob).stream().map(row -> row.charAt(3)).toList());
        }
    }

    /**
     * Visit counts in display order: application, HTTP session, Vaadin session,
     * browser tab, UI, route.
     */
    private static List<Integer> visits(ComponentQuery<Span> spans) {
        return spans.withClassName("scope-badge").all().stream()
                .map(s -> Integer.parseInt(s.getText())).toList();
    }

    private static List<Boolean> startedOver(ComponentQuery<Span> spans) {
        return spans.withClassName("scope-badge").all().stream()
                .map(s -> s.hasClassName("started-over")).toList();
    }

    /**
     * The ✓/✗ marks of each row, in display order, one character per action.
     */
    private static List<String> marks(BrowserlessUIContext window) {
        List<String> cells = window.find(Div.class).all().stream()
                .filter(d -> d.hasClassName("kept") || d.hasClassName("lost"))
                .map(Div::getText).toList();
        return IntStream.range(0, cells.size() / 5).mapToObj(
                row -> String.join("", cells.subList(row * 5, row * 5 + 5)))
                .toList();
    }
}
