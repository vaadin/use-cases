package com.example.uc5;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.example.MissingAPI;
import com.example.MissingAPI.NotificationOptions;
import com.example.MissingAPI.NotificationPermission;
import com.example.views.MainLayout;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.PageVisibility;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC5 — Only notify when the user isn't looking at the tab.
 * <p>
 * An OS notification for something the user is already looking at is noise.
 * This view combines the notification permission with
 * {@link com.vaadin.flow.component.page.Page#pageVisibilitySignal()}: when a
 * message arrives while the tab is visible and focused, it is shown as an
 * in-app Vaadin {@link Notification}; when the tab is hidden or the window has
 * lost focus, it becomes a native OS notification instead. Unlike Web Push,
 * this only works while the tab is open — for delivery to closed tabs see the
 * Web Push use case in the page-visibility module.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Only when hidden")
@Menu(order = 5, title = "UC5 — Only when hidden")
public class HiddenTabOnlyView extends VerticalLayout {

    static final Duration DELAY = Duration.ofSeconds(5);
    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    private final TaskScheduler taskScheduler;
    private final Span visibilityBadge = new Span();
    private final Span permissionBadge = new Span();
    private final Div log = new Div();

    public HiddenTabOnlyView(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;

        add(new H1("UC5 — Only notify when the tab is hidden"));
        add(new Paragraph("Click \"Send me a message in 5 seconds\". Stay "
                + "on this tab and you get an in-app notification; switch "
                + "tabs or focus another window and you get an OS "
                + "notification instead."));

        Button allow = new Button("Allow notifications");
        MissingAPI.requestPermissionOnClick(allow);
        allow.bindEnabled(MissingAPI.permissionSignal(UI.getCurrent())
                .map(p -> p == NotificationPermission.DEFAULT));
        Button send = new Button("Send me a message in 5 seconds",
                e -> schedule(UI.getCurrent()));
        send.addThemeVariants(ButtonVariant.PRIMARY);

        visibilityBadge.addClassName("status-badge");
        permissionBadge.addClassName("status-badge");
        HorizontalLayout status = new HorizontalLayout(new Span("Tab:"),
                visibilityBadge, new Span("Notifications:"),
                permissionBadge);
        status.setAlignItems(Alignment.CENTER);

        log.addClassName("notification-log");
        add(new HorizontalLayout(allow, send), status,
                new H2("Delivery log"), log);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        Signal<PageVisibility> visibility = ui.getPage()
                .pageVisibilitySignal();
        Signal<NotificationPermission> permission = MissingAPI
                .permissionSignal(ui);

        visibilityBadge.bindText(visibility.map(v -> v.name().toLowerCase()
                .replace('_', ' ')));
        visibilityBadge.bindClassName("hidden",
                visibility.map(HiddenTabOnlyView::userIsAway));
        permissionBadge
                .bindText(permission.map(p -> p.name().toLowerCase()));
        permissionBadge.bindClassName("paused",
                permission.map(p -> p != NotificationPermission.GRANTED));
    }

    private void schedule(UI ui) {
        Notification.show("A message will arrive in 5 seconds", 2000,
                Notification.Position.BOTTOM_START);
        taskScheduler.schedule(ui.accessLater(() -> deliver(ui), null),
                Instant.now().plus(DELAY));
    }

    // Package-private so tests can drive the gating logic directly.
    void deliver(UI ui) {
        PageVisibility visibility = ui.getPage().pageVisibilitySignal()
                .peek();
        NotificationPermission permission = MissingAPI.permissionSignal(ui)
                .peek();
        String channel;
        if (!userIsAway(visibility)) {
            inApp();
            channel = "in-app (user is looking)";
        } else if (permission == NotificationPermission.GRANTED) {
            MissingAPI.showNotification(ui, "New message from Alice",
                    NotificationOptions.body("Did you see the new designs?")
                            .withTag("uc5-message"))
                    .onError(reason -> {
                        inApp();
                        appendLog("OS notification refused (" + reason
                                + ") → in-app");
                    });
            channel = "OS notification (user is away)";
        } else {
            inApp();
            channel = "in-app (user is away, but permission is "
                    + permission.name().toLowerCase() + ")";
        }
        appendLog("tab=" + visibility + "  →  " + channel);
    }

    private static boolean userIsAway(PageVisibility visibility) {
        return visibility == PageVisibility.HIDDEN
                || visibility == PageVisibility.VISIBLE_NOT_FOCUSED;
    }

    private static void inApp() {
        Notification.show("New message from Alice: Did you see the new "
                + "designs?", 4000, Notification.Position.TOP_END);
    }

    private void appendLog(String line) {
        log.addComponentAsFirst(
                new Div(LocalTime.now().format(TIME) + "  " + line));
    }
}
