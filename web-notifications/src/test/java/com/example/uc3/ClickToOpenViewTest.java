package com.example.uc3;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import com.example.WebNotificationSimulator;
import com.example.WebNotificationSimulator.ShownNotification;
import com.example.home.HomeView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.webnotification.WebNotificationPermission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ViewPackages(classes = { ClickToOpenView.class, HomeView.class })
class ClickToOpenViewTest extends SpringBrowserlessTest {

    // The tests assign tickets by hand instead of waiting five seconds.
    @MockitoBean
    private TaskScheduler taskScheduler;

    private final ScheduledFuture<?> scheduled = mock(ScheduledFuture.class);

    @BeforeEach
    void returnScheduledFuture() {
        doReturn(scheduled).when(taskScheduler).schedule(any(Runnable.class),
                any(Instant.class));
    }

    @Test
    void viewRendersHeadingAndButtons() {
        navigate(ClickToOpenView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> h.getText().equals("UC3 — Click to open")));
        assertTrue(findInView(Button.class).all().stream().anyMatch(
                b -> b.getText().equals("Assign me a ticket in 5 seconds")));
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "Nothing opened yet.".equals(s.getText())));
    }

    @Test
    void clickingTheNotificationOpensTheTicket() {
        ClickToOpenView view = navigate(ClickToOpenView.class);
        WebNotificationSimulator
                .setPermission(WebNotificationPermission.GRANTED);
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
        // The browser closes the notification after the click.
        assertTrue(WebNotificationSimulator.shown().isEmpty());
    }

    @Test
    void withoutPermissionTheInAppOpenButtonOpensTheTicket() {
        ClickToOpenView view = navigate(ClickToOpenView.class);
        WebNotificationSimulator
                .setPermission(WebNotificationPermission.DENIED);
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

    @Test
    void leavingTheViewCancelsThePendingAssignment() {
        navigate(ClickToOpenView.class);

        test(findInView(Button.class).all().stream()
                .filter(b -> "Assign me a ticket in 5 seconds"
                        .equals(b.getText()))
                .findFirst().orElseThrow()).click();
        navigate(HomeView.class);

        verify(scheduled).cancel(false);
    }
}
