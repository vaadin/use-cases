package com.example.uc4;

import com.example.MissingAPI.NotificationPermission;
import com.example.WebNotificationSimulator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = TaggedCounterView.class)
class TaggedCounterViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndCounter() {
        navigate(TaggedCounterView.class);
        runPendingSignalsTasks();

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> h.getText().startsWith("UC4 — Collapse repeated")));
        assertSpan("0 unread");
        assertFalse(button("Mark all as read").isEnabled());
    }

    @Test
    void taggedNotificationsReplaceEachOther() {
        navigate(TaggedCounterView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.GRANTED);
        runPendingSignalsTasks();

        test(button("Receive 3 in a burst")).click();
        runPendingSignalsTasks();

        assertSpan("3 unread");
        assertLogContains("\"3 new messages\"  tag=inbox");
        // Same tag: only the latest notification is still live.
        assertEquals(1, WebNotificationSimulator.shown().size());
        assertEquals("3 new messages",
                WebNotificationSimulator.shown().get(0).title());

        test(button("Mark all as read")).click();
        runPendingSignalsTasks();
        assertSpan("0 unread");
        assertTrue(WebNotificationSimulator.shown().isEmpty());
    }

    @Test
    void untaggedNotificationsStack() {
        navigate(TaggedCounterView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.GRANTED);
        runPendingSignalsTasks();

        test(findInView(Checkbox.class).single()).click();
        test(button("Receive 3 in a burst")).click();
        runPendingSignalsTasks();

        assertLogContains("tag=(none)");
        assertEquals(3, WebNotificationSimulator.shown().size());
    }

    @Test
    void withoutPermissionOnlyTheCounterUpdates() {
        navigate(TaggedCounterView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.DEFAULT);
        runPendingSignalsTasks();

        test(button("Receive a message")).click();
        runPendingSignalsTasks();

        assertSpan("1 unread");
        assertLogContains("not shown — permission is default");
        assertTrue(WebNotificationSimulator.shown().isEmpty());
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }

    private void assertSpan(String text) {
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> text.equals(s.getText())),
                "expected span \"" + text + "\"");
    }

    private void assertLogContains(String fragment) {
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getText() != null
                        && d.getText().contains(fragment)),
                "expected log to contain \"" + fragment + "\"");
    }
}
