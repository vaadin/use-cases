package com.example.uc3;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.example.views.MainLayout;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.PageVisibility;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webpush.WebPush;
import com.vaadin.flow.server.webpush.WebPushException;
import com.vaadin.flow.server.webpush.WebPushMessage;
import com.vaadin.flow.server.webpush.WebPushSubscription;

/**
 * UC3 — Notification gating with Web Push.
 * <p>
 * Demonstrates choosing the right delivery channel based on
 * {@link com.vaadin.flow.component.page.Page#pageVisibilitySignal()}: an
 * in-page toast is enough when the user is looking at the tab, but a Web Push
 * notification is needed when they aren't.
 */
@Route(value = "uc3", layout = MainLayout.class)
@Menu(order = 3, title = "UC3 — Notification gating")
@StyleSheet("uc3.css")
public class NotificationGatingView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    private final WebPush webPush;
    private final TaskScheduler taskScheduler;

    private final Span subscriptionStatus = new Span("Not subscribed");
    private final Div log = new Div();

    public NotificationGatingView(WebPush webPush,
            TaskScheduler taskScheduler) {
        this.webPush = webPush;
        this.taskScheduler = taskScheduler;

        addClassName("uc3-view");
        add(new H1("UC3 — Notification gating with Web Push"));
        add(new Paragraph("Subscribe the browser, then click \"Send in 5 "
                + "seconds\" and immediately switch tabs or hide the "
                + "window. The 5-second timer fires server-side, "
                + "inspects the visibility signal, and picks the "
                + "appropriate channel: in-tab toast when visible, OS "
                + "notification via Web Push otherwise."));

        Button subscribe = new Button("Enable browser notifications");
        subscribe.addThemeVariants(ButtonVariant.PRIMARY);
        subscribe.addClickListener(e -> subscribe(UI.getCurrent()));

        Button unsubscribe = new Button("Disable",
                e -> unsubscribe(UI.getCurrent()));

        Button send = new Button("Send me a notification in 5 seconds");
        send.addClickListener(e -> scheduleNotification(UI.getCurrent()));

        subscriptionStatus.addClassName("status-badge");
        subscriptionStatus.addClassName("paused");

        add(new HorizontalLayout(subscribe, unsubscribe), subscriptionStatus,
                new H2("Trigger"), send, new H2("Delivery log"));

        log.addClassName("notification-log");
        add(log);
        appendLog("(no notifications fired yet)");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        // Refresh subscription status from the browser when the view loads.
        webPush.subscriptionExists(ui, exists -> ui.access(
                () -> renderSubscribed(exists && hasStoredSubscription())));
    }

    private void subscribe(UI ui) {
        webPush.subscribe(ui, subscription -> ui.access(() -> {
            if (subscription == null) {
                Notification.show("Permission denied or subscription failed",
                        4000, Position.MIDDLE);
                renderSubscribed(false);
                return;
            }
            VaadinSession.getCurrent().setAttribute(WebPushSubscription.class,
                    subscription);
            renderSubscribed(true);
            Notification.show("Subscribed to web push", 2500, Position.MIDDLE);
        }));
    }

    private void unsubscribe(UI ui) {
        webPush.unsubscribe(ui, subscription -> ui.access(() -> {
            VaadinSession.getCurrent().setAttribute(WebPushSubscription.class,
                    null);
            renderSubscribed(false);
            Notification.show("Unsubscribed", 2000, Position.MIDDLE);
        }));
    }

    private void scheduleNotification(UI ui) {
        Notification.show(
                "Will fire in 5 seconds — switch tab now if "
                        + "you want to test web push",
                2500, Position.BOTTOM_START);
        taskScheduler.schedule(ui.accessLater(() -> fireNotification(ui), null),
                Instant.now().plusSeconds(5));
    }

    // Package-private so tests can drive the gating logic without waiting on
    // the real 5-second scheduler delay.
    void fireNotification(UI ui) {
        PageVisibility state = ui.getPage().pageVisibilitySignal().peek();
        WebPushSubscription subscription = VaadinSession.getCurrent()
                .getAttribute(WebPushSubscription.class);

        String channel;
        if (state == PageVisibility.VISIBLE) {
            Notification.show("New message from Alice", 4000, Position.MIDDLE);
            channel = "in-tab";
        } else if (subscription != null) {
            try {
                webPush.sendNotification(subscription, new WebPushMessage(
                        "New message", "Alice sent you a message"));
                channel = "web push";
            } catch (WebPushException ex) {
                Notification.show(
                        "Web push failed: " + ex.getMessage() + " — falling "
                                + "back to in-tab toast",
                        4000, Position.MIDDLE);
                channel = "web push failed → in-tab fallback";
            }
        } else {
            Notification.show(
                    "New message from Alice (subscribe to receive "
                            + "OS notifications when the tab is hidden)",
                    4000, Position.MIDDLE);
            channel = "no subscription → in-tab fallback";
        }
        appendLog(LocalTime.now().format(TIME) + "  state=" + state + "  →  "
                + channel);
    }

    private void renderSubscribed(boolean subscribed) {
        subscriptionStatus.removeClassNames("paused", "hidden");
        if (subscribed) {
            subscriptionStatus.setText("Subscribed");
        } else {
            subscriptionStatus.setText("Not subscribed");
            subscriptionStatus.addClassName("paused");
        }
    }

    private boolean hasStoredSubscription() {
        return VaadinSession.getCurrent()
                .getAttribute(WebPushSubscription.class) != null;
    }

    private void appendLog(String line) {
        Div entry = new Div();
        entry.setText(line);
        log.addComponentAsFirst(entry);
    }
}
