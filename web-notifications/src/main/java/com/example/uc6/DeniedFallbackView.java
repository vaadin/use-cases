package com.example.uc6;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.webnotification.WebNotification;
import com.vaadin.flow.component.webnotification.WebNotificationOptions;
import com.vaadin.flow.component.webnotification.WebNotificationPermission;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC6 — Denied permission.
 * <p>
 * Once the user blocks notifications, the browser never shows the prompt again
 * — calling {@code requestPermission()} just resolves to {@code "denied"}. An
 * app shouldn't keep offering a button that does nothing. This view detects the
 * denied state, explains how to re-enable notifications in the common browsers,
 * and keeps working by falling back to in-app Vaadin notifications. When the
 * user re-enables notifications in the site settings, the view notices (the
 * browser reports the change, at the latest when the user returns to the tab)
 * and switches back to OS notifications without a reload.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Denied permission")
@UseCaseDescription("Explaining how to re-enable blocked notifications and falling back to in-app messages")
@Menu(order = 6, title = "UC6 — Denied permission")
@StyleSheet("uc6.css")
public class DeniedFallbackView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    private final Button allow = new Button("Allow notifications");
    private final Span statusBadge = new Span();
    private final Div helpPanel = new Div();
    private final Div log = new Div();

    public DeniedFallbackView() {
        add(new H1("UC6 — Denied permission"));
        add(new Paragraph("To try this, click \"Allow notifications\" and "
                + "choose Block (or block notifications for this site in "
                + "the browser settings). The view explains how to undo "
                + "it and keeps delivering messages in-app meanwhile."));

        WebNotification.onClick(allow).requestPermission();
        allow.addThemeVariants(ButtonVariant.PRIMARY);
        Button send = new Button("Send me a notification",
                e -> send(UI.getCurrent()));
        statusBadge.addClassName("status-badge");

        HorizontalLayout row = new HorizontalLayout(allow, send, statusBadge);
        row.setAlignItems(Alignment.CENTER);
        row.setWrap(true);

        buildHelpPanel();
        log.addClassName("notification-log");
        add(row, helpPanel, new H2("Delivery log"), log);

        Signal<WebNotificationPermission> permission = WebNotification
                .permissionSignal();
        helpPanel.bindVisible(
                permission.map(p -> p == WebNotificationPermission.DENIED));
        allow.bindVisible(
                permission.map(p -> p == WebNotificationPermission.PROMPT
                        || p == WebNotificationPermission.UNKNOWN));
        allow.bindEnabled(
                permission.map(p -> p == WebNotificationPermission.PROMPT));
        statusBadge.bindText(permission.map(p -> switch (p) {
        case GRANTED -> "OS notifications on";
        case DENIED -> "Blocked — using in-app notifications";
        case UNSUPPORTED -> "Unsupported — using in-app notifications";
        case PROMPT -> "Not decided — using in-app notifications";
        case UNKNOWN -> "Checking…";
        }));
        statusBadge.bindClassName("hidden",
                permission.map(p -> p == WebNotificationPermission.DENIED));
        statusBadge.bindClassName("paused",
                permission.map(p -> p != WebNotificationPermission.GRANTED
                        && p != WebNotificationPermission.DENIED));
    }

    private void buildHelpPanel() {
        helpPanel.addClassName("uc6-help-panel");
        UnorderedList steps = new UnorderedList(
                new ListItem("Chrome / Edge: click the icon left of the "
                        + "address bar → Site settings → Notifications → "
                        + "Allow."),
                new ListItem("Firefox: click the icon left of the address "
                        + "bar → clear \"Blocked\" next to Send "
                        + "notifications, then allow when asked."),
                new ListItem("Safari (macOS): Safari → Settings → Websites "
                        + "→ Notifications → set this site to Allow."),
                new ListItem("Also check that notifications for the browser "
                        + "itself are enabled in the operating system "
                        + "settings."));
        helpPanel.add(new H3("Notifications are blocked"),
                new Paragraph("Your browser won't ask again, so the "
                        + "\"Allow\" button can't help any more. You'll "
                        + "keep getting messages inside the app. To get "
                        + "OS notifications again:"),
                steps, new Paragraph("Then come back to this tab: the view "
                        + "notices the change on its own."));
    }

    // Package-private so tests can call it without a click.
    void send(UI ui) {
        WebNotificationPermission permission = WebNotification
                .permissionSignal(ui).peek();
        if (permission == WebNotificationPermission.GRANTED) {
            WebNotification
                    .show("Build #218 passed",
                            WebNotificationOptions.builder()
                                    .body("All 1,204 tests green.").build(),
                            ui)
                    .addErrorListener(error -> {
                        inApp();
                        appendLog("OS notification refused (" + error.reason()
                                + ") → in-app fallback");
                    });
            appendLog("OS notification");
        } else {
            inApp();
            appendLog("in-app fallback (permission "
                    + permission.name().toLowerCase() + ")");
        }
    }

    private static void inApp() {
        Notification.show("Build #218 passed — all 1,204 tests green.", 4000,
                Notification.Position.TOP_END);
    }

    private void appendLog(String line) {
        log.addComponentAsFirst(
                new Div(LocalTime.now().format(TIME) + "  " + line));
    }
}
