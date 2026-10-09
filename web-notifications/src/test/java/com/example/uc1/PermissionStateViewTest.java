package com.example.uc1;

import com.example.MissingAPI.NotificationPermission;
import com.example.WebNotificationSimulator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PermissionStateView.class)
class PermissionStateViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndButtons() {
        navigate(PermissionStateView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(h -> h
                .getText().equals("UC1 — Ask for permission on a user gesture")));
        assertEquals(1, findInView(Button.class).all().stream()
                .filter(b -> "Allow notifications".equals(b.getText())).count());
    }

    @Test
    void badgeAndButtonsFollowPermission() {
        navigate(PermissionStateView.class);
        runPendingSignalsTasks();

        // Before the browser reports, nothing can be requested or sent.
        assertBadge("Checking…");
        assertFalse(button("Allow notifications").get().isEnabled());
        assertFalse(button("Send a test notification").get().isEnabled());

        setPermission(NotificationPermission.DEFAULT);
        assertBadge("Not decided (default)");
        assertTrue(button("Allow notifications").get().isEnabled());

        setPermission(NotificationPermission.DENIED);
        assertBadge("Blocked (denied)");
        assertFalse(button("Allow notifications").get().isEnabled());

        setPermission(NotificationPermission.UNSUPPORTED);
        assertBadge("Not supported");

        setPermission(NotificationPermission.GRANTED);
        assertBadge("Allowed (granted)");
        assertFalse(button("Allow notifications").get().isEnabled());
        assertTrue(button("Send a test notification").get().isEnabled());
    }

    @Test
    void testButtonShowsNotificationWhenGranted() {
        navigate(PermissionStateView.class);
        setPermission(NotificationPermission.GRANTED);

        test(button("Send a test notification").get()).click();

        assertEquals(1, WebNotificationSimulator.shown().size());
        assertEquals("Hello from Vaadin",
                WebNotificationSimulator.shown().get(0).title());
    }

    private java.util.Optional<Button> button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst();
    }

    private void assertBadge(String text) {
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> text.equals(s.getText())),
                "expected badge \"" + text + "\"");
    }

    private void setPermission(NotificationPermission permission) {
        WebNotificationSimulator.setPermission(permission);
        runPendingSignalsTasks();
    }
}
