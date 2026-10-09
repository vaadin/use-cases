package com.example.uc2;

import com.example.MissingAPI.NotificationPermission;
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
import com.vaadin.flow.component.progressbar.ProgressBar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LongTaskView.class)
class LongTaskViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndStartButton() {
        navigate(LongTaskView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(h -> h
                .getText().startsWith("UC2 — Notify when a long-running")));
        assertTrue(startButton().isEnabled());
        // findInView only returns visible components.
        assertTrue(findInView(ProgressBar.class).all().isEmpty());
    }

    @Test
    void startingDisablesButtonAndFinishingSendsOsNotification() {
        LongTaskView view = navigate(LongTaskView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.GRANTED);
        runPendingSignalsTasks();

        test(startButton()).click();
        runPendingSignalsTasks();
        assertFalse(startButton().isEnabled());
        assertTrue(findInView(ProgressBar.class).single().isVisible());

        view.finish(UI.getCurrent());
        runPendingSignalsTasks();
        assertTrue(startButton().isEnabled());
        assertSpan("Report ready.");
        assertLogContains("→ OS notification");
        assertEquals("Your report is ready",
                WebNotificationSimulator.shown().get(0).title());
    }

    @Test
    void finishingWithoutPermissionFallsBackToInApp() {
        LongTaskView view = navigate(LongTaskView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.DENIED);
        runPendingSignalsTasks();

        view.finish(UI.getCurrent());
        runPendingSignalsTasks();
        assertLogContains("→ in-app notification");
        assertTrue(WebNotificationSimulator.shown().isEmpty());
    }

    @Test
    void browserRefusalFallsBackToInApp() {
        LongTaskView view = navigate(LongTaskView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.GRANTED);
        view.finish(UI.getCurrent());

        WebNotificationSimulator.fail(WebNotificationSimulator.shown().get(0),
                "TypeError");
        assertLogContains("OS notification refused (TypeError) → in-app");
    }

    private Button startButton() {
        return findInView(Button.class).all().stream()
                .filter(b -> "Generate report".equals(b.getText()))
                .findFirst().orElseThrow();
    }

    private void assertSpan(String text) {
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> text.equals(s.getText())));
    }

    private void assertLogContains(String fragment) {
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getText() != null
                        && d.getText().contains(fragment)),
                "expected log to contain \"" + fragment + "\"");
    }
}
