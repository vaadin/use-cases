package com.example.uc4;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.webnotification.WebNotification;
import com.vaadin.flow.component.webnotification.WebNotificationOptions;
import com.vaadin.flow.component.webnotification.WebNotificationPermission;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — Collapse repeated alerts.
 * <p>
 * A chat app that raises one OS notification per incoming message quickly
 * buries the user under a stack of near-identical popups. Giving every
 * notification the same {@code tag} makes the browser <em>replace</em> the
 * previous one instead, so the user sees a single, up-to-date "5 new messages"
 * notification. {@code renotify} makes the replacement alert again (on browsers
 * that support it). Untick "Collapse with tag" to compare with the stacking
 * behaviour; "Mark all as read" resets the counter and closes the tagged
 * notification.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Collapse repeated alerts")
@UseCaseDescription("Replacing one notification instead of stacking a new one for every message")
@Menu(order = 4, title = "UC4 — Collapse repeated alerts")
@StyleSheet("uc4.css")
public class TaggedCounterView extends VerticalLayout {

    static final String TAG = "inbox";
    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");
    private static final List<String> SENDERS = List.of("Alice", "Bob", "Carol",
            "Dave");
    private static final List<String> MESSAGES = List.of(
            "Are we still on for 3pm?", "I pushed the fix.",
            "Can you review my PR?", "Lunch?");

    private final ValueSignal<Integer> unread = new ValueSignal<>(0);
    private final Checkbox collapse = new Checkbox("Collapse with tag", true);
    private final Div log = new Div();
    private int received;

    public TaggedCounterView() {
        add(new H1("UC4 — Collapse repeated alerts"));
        add(new Paragraph("Enable notifications, then receive a few "
                + "messages. With \"Collapse with tag\" ticked the OS "
                + "shows one notification whose counter goes up; "
                + "unticked, every message stacks its own notification."));

        Button allow = new Button("Allow notifications");
        WebNotification.onClick(allow).requestPermission();
        allow.bindEnabled(WebNotification.permissionSignal()
                .map(p -> p == WebNotificationPermission.PROMPT));

        Button receiveOne = new Button("Receive a message",
                e -> receive(UI.getCurrent(), 1));
        receiveOne.addThemeVariants(ButtonVariant.PRIMARY);
        Button receiveBurst = new Button("Receive 3 in a burst",
                e -> receive(UI.getCurrent(), 3));
        Button markRead = new Button("Mark all as read",
                e -> markAllRead(UI.getCurrent()));

        Span counter = new Span();
        counter.addClassName("uc4-counter");
        counter.bindText(unread.map(n -> n + " unread"));

        log.addClassName("notification-log");
        HorizontalLayout settings = new HorizontalLayout(allow, collapse);
        settings.setAlignItems(Alignment.CENTER);
        settings.setWrap(true);
        HorizontalLayout actions = new HorizontalLayout(receiveOne,
                receiveBurst, markRead);
        actions.setWrap(true);
        add(settings, actions, counter, new H2("Notifications sent"), log);
        markRead.bindEnabled(unread.map(n -> n > 0));
    }

    private void receive(UI ui, int count) {
        for (int i = 0; i < count; i++) {
            String sender = SENDERS.get(received % SENDERS.size());
            String text = MESSAGES.get(received % MESSAGES.size());
            received++;
            unread.set(unread.peek() + 1);
            int total = unread.peek();
            notifyMessage(ui, total, sender, text);
        }
    }

    private void notifyMessage(UI ui, int total, String sender, String text) {
        boolean useTag = collapse.getValue();
        String title = useTag
                ? total + (total == 1 ? " new message" : " new messages")
                : "Message from " + sender;
        WebNotificationOptions.Builder options = WebNotificationOptions
                .builder().body(sender + ": " + text);
        if (useTag) {
            options.tag(TAG).renotify(true);
        }
        WebNotificationPermission permission = WebNotification
                .permissionSignal(ui).peek();
        if (permission == WebNotificationPermission.GRANTED) {
            WebNotification.show(title, options.build(), ui);
            appendLog("\"" + title + "\"  tag=" + (useTag ? TAG : "(none)"));
        } else {
            appendLog("\"" + title + "\"  not shown — permission is "
                    + permission.name().toLowerCase());
        }
    }

    private void markAllRead(UI ui) {
        unread.set(0);
        WebNotification.closeByTag(TAG, ui);
        appendLog("marked all as read → closed tag=" + TAG);
    }

    private void appendLog(String line) {
        log.addComponentAsFirst(
                new Div(LocalTime.now().format(TIME) + "  " + line));
    }
}
