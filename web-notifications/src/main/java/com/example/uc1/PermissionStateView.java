package com.example.uc1;

import com.example.MissingAPI;
import com.example.MissingAPI.NotificationOptions;
import com.example.MissingAPI.NotificationPermission;
import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC1 — Ask for permission on a user gesture.
 * <p>
 * Browsers only show the notification permission prompt when
 * {@code Notification.requestPermission()} is called from inside a user
 * gesture such as a click, and they penalise sites that prompt on page load.
 * This view asks only when the user clicks "Allow notifications". The request
 * is bound to the button on the client, because a round-trip to the server
 * would lose the gesture. The current permission is shown reactively:
 * {@code default} (not decided yet), {@code granted}, {@code denied}, or
 * "unsupported" for browsers without the API or pages that aren't served over
 * HTTPS. Once permission is granted, a test notification can be sent.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Ask for permission")
@Menu(order = 1, title = "UC1 — Ask for permission")
public class PermissionStateView extends VerticalLayout {

    private final Button allowButton = new Button("Allow notifications",
            VaadinIcon.BELL.create());
    private final Button testButton = new Button("Send a test notification");
    private final Span statusBadge = new Span();
    private final Paragraph explanation = new Paragraph();

    public PermissionStateView() {
        add(new H1("UC1 — Ask for permission on a user gesture"));
        add(new Paragraph("Nothing is asked when the page loads. Click the "
                + "button to trigger the browser's permission prompt — the "
                + "request runs inside your click, which browsers require. "
                + "The badge follows the permission live, including changes "
                + "you make later in the browser's site settings."));

        allowButton.addThemeVariants(ButtonVariant.PRIMARY);
        statusBadge.addClassName("status-badge");

        HorizontalLayout row = new HorizontalLayout(allowButton, statusBadge);
        row.setAlignItems(Alignment.CENTER);
        add(row, explanation, new H2("Try it"), testButton);

        // Bound on the client: requestPermission() runs in the click gesture.
        MissingAPI.requestPermissionOnClick(allowButton);
        testButton.addClickListener(e -> MissingAPI.showNotification(
                UI.getCurrent(), "Hello from Vaadin",
                NotificationOptions.body("Notifications are working.")));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        Signal<NotificationPermission> permission = MissingAPI
                .permissionSignal(ui);

        statusBadge.bindText(permission.map(PermissionStateView::badgeText));
        statusBadge.bindClassName("paused",
                permission.map(p -> p == NotificationPermission.DEFAULT
                        || p == NotificationPermission.UNKNOWN));
        statusBadge.bindClassName("hidden",
                permission.map(p -> p == NotificationPermission.DENIED
                        || p == NotificationPermission.UNSUPPORTED));
        explanation.bindText(permission.map(PermissionStateView::explain));

        // Only a "default" permission can still be prompted for.
        allowButton.bindEnabled(
                permission.map(p -> p == NotificationPermission.DEFAULT));
        testButton.bindEnabled(
                permission.map(p -> p == NotificationPermission.GRANTED));
    }

    static String badgeText(NotificationPermission permission) {
        return switch (permission) {
        case UNKNOWN -> "Checking…";
        case DEFAULT -> "Not decided (default)";
        case GRANTED -> "Allowed (granted)";
        case DENIED -> "Blocked (denied)";
        case UNSUPPORTED -> "Not supported";
        };
    }

    private static String explain(NotificationPermission permission) {
        return switch (permission) {
        case UNKNOWN -> "Waiting for the browser to report the permission.";
        case DEFAULT -> "You haven't decided yet. Clicking the button shows "
                + "the browser's prompt.";
        case GRANTED -> "This site may show notifications. The browser won't "
                + "prompt again, so the button is disabled.";
        case DENIED -> "Notifications are blocked. Browsers never prompt "
                + "again after a denial — re-enable them from the site "
                + "settings (the icon left of the address bar).";
        case UNSUPPORTED -> "This browser has no Notification API, or the "
                + "page isn't served over HTTPS (or localhost).";
        };
    }
}
