package com.example.uc1;

import com.example.PageVisibilityTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PageVisibility;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UpdateWhenActiveView.class)
class UpdateWhenActiveViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithExpectedHeadings() {
        navigate(UpdateWhenActiveView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC1 — Update when active".equals(h.getText())));
        assertTrue(findInView(H2.class).all().stream()
                .anyMatch(h -> "Status".equals(h.getText())));
    }

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
