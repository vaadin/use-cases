package com.example.uc5;

import com.example.MissingAPI.NotificationPermission;
import com.example.PageVisibilityTestSupport;
import com.example.WebNotificationSimulator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PageVisibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = HiddenTabOnlyView.class)
class HiddenTabOnlyViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndBadges() {
        navigate(HiddenTabOnlyView.class);
        runPendingSignalsTasks();

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> h.getText().startsWith("UC5 — Only notify when")));
        assertTrue(findInView(Button.class).all().stream().anyMatch(b -> b
                .getText().equals("Send me a message in 5 seconds")));

        setVisibility(PageVisibility.HIDDEN);
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "hidden".equals(s.getText())));
    }

    @Test
    void channelDependsOnVisibilityAndPermission() {
        HiddenTabOnlyView view = navigate(HiddenTabOnlyView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.GRANTED);
        runPendingSignalsTasks();

        setVisibility(PageVisibility.VISIBLE);
        view.deliver(UI.getCurrent());
        assertLogContains("tab=VISIBLE  →  in-app (user is looking)");
        assertTrue(WebNotificationSimulator.shown().isEmpty());

        setVisibility(PageVisibility.HIDDEN);
        view.deliver(UI.getCurrent());
        assertLogContains("tab=HIDDEN  →  OS notification (user is away)");
        assertEquals(1, WebNotificationSimulator.shown().size());

        setVisibility(PageVisibility.VISIBLE_NOT_FOCUSED);
        view.deliver(UI.getCurrent());
        assertLogContains(
                "tab=VISIBLE_NOT_FOCUSED  →  OS notification (user is away)");
    }

    @Test
    void awayWithoutPermissionFallsBackToInApp() {
        HiddenTabOnlyView view = navigate(HiddenTabOnlyView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.DENIED);
        setVisibility(PageVisibility.HIDDEN);

        view.deliver(UI.getCurrent());
        assertLogContains("in-app (user is away, but permission is denied)");
        assertTrue(WebNotificationSimulator.shown().isEmpty());
    }

    private void setVisibility(PageVisibility state) {
        PageVisibilityTestSupport.setPageVisibility(state);
        runPendingSignalsTasks();
    }

    private void assertLogContains(String fragment) {
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getText() != null
                        && d.getText().contains(fragment)),
                "expected log to contain \"" + fragment + "\"");
    }
}
