package com.example.uc3;

import com.example.MissingAPI.NotificationPermission;
import com.example.MissingAPI.ShownNotification;
import com.example.WebNotificationSimulator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ClickToOpenView.class)
class ClickToOpenViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndButtons() {
        navigate(ClickToOpenView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> h.getText().startsWith("UC3 — Click a notification")));
        assertTrue(findInView(Button.class).all().stream().anyMatch(b -> b
                .getText().equals("Assign me a ticket in 5 seconds")));
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "Nothing opened yet.".equals(s.getText())));
    }

    @Test
    void clickingTheNotificationOpensTheTicket() {
        ClickToOpenView view = navigate(ClickToOpenView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.GRANTED);
        runPendingSignalsTasks();

        ClickToOpenView.Ticket ticket = view.assignTicket(UI.getCurrent());
        runPendingSignalsTasks();

        assertEquals(1, WebNotificationSimulator.shown().size());
        ShownNotification shown = WebNotificationSimulator.shown().get(0);
        assertEquals("Ticket #" + ticket.number() + " assigned to you",
                shown.title());
        // Not opened until the user clicks.
        assertTrue(findInView(H3.class).all().isEmpty());

        WebNotificationSimulator.click(shown);
        runPendingSignalsTasks();

        assertTrue(findInView(H3.class).all().stream().anyMatch(
                h -> h.getText().equals("Ticket #" + ticket.number())));
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getText() != null && s.getText()
                        .startsWith("Opened from notification click")));
        // The shim closes the notification after the click.
        assertTrue(WebNotificationSimulator.shown().isEmpty());
    }

    @Test
    void withoutPermissionTheInAppOpenButtonOpensTheTicket() {
        ClickToOpenView view = navigate(ClickToOpenView.class);
        WebNotificationSimulator.setPermission(NotificationPermission.DENIED);
        runPendingSignalsTasks();

        ClickToOpenView.Ticket ticket = view.assignTicket(UI.getCurrent());
        assertTrue(WebNotificationSimulator.shown().isEmpty());

        Button open = find(Button.class).all().stream()
                .filter(b -> "Open".equals(b.getText())).findFirst()
                .orElseThrow();
        test(open).click();
        runPendingSignalsTasks();

        assertTrue(findInView(H3.class).all().stream().anyMatch(
                h -> h.getText().equals("Ticket #" + ticket.number())));
    }
}
