package com.example.uc4;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.MissingAPI;
import com.example.MissingAPI.NotificationOptions;
import com.example.MissingAPI.NotificationPermission;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — Collapse repeated notifications with a tag.
 * <p>
 * A chat app that raises one OS notification per incoming message quickly
 * buries the user under a stack of near-identical popups. Giving every
 * notification the same {@code tag} makes the browser <em>replace</em> the
 * previous one instead, so the user sees a single, up-to-date "5 new
 * messages" notification. {@code renotify} makes the replacement alert again
 * (on browsers that support it). Untick "Collapse with tag" to compare with
 * the stacking behaviour; "Mark all as read" resets the counter and closes
 * the tagged notification.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Collapse with tag")
@Menu(order = 4, title = "UC4 — Collapse with tag")
public class TaggedCounterView extends VerticalLayout {

    static final String TAG = "inbox";
    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");
    private static final List<String> SENDERS = List.of("Alice", "Bob",
            "Carol", "Dave");
    private static final List<String> MESSAGES = List.of(
            "Are we still on for 3pm?", "I pushed the fix.",
            "Can you review my PR?", "Lunch?");

    private final ValueSignal<Integer> unread = new ValueSignal<>(0);
    private final Checkbox collapse = new Checkbox("Collapse with tag", true);
    private final Div log = new Div();
    private int received;

    public TaggedCounterView() {
        add(new H1("UC4 — Collapse repeated notifications with a tag"));
        add(new Paragraph("Enable notifications, then receive a few "
                + "messages. With \"Collapse with tag\" ticked the OS "
                + "shows one notification whose counter goes up; "
                + "unticked, every message stacks its own notification."));

        Button allow = new Button("Allow notifications");
        MissingAPI.requestPermissionOnClick(allow);
        allow.bindEnabled(MissingAPI.permissionSignal(UI.getCurrent())
                .map(p -> p == NotificationPermission.DEFAULT));

        Button receiveOne = new Button("Receive a message",
                e -> receive(UI.getCurrent(), 1));
        receiveOne.addThemeVariants(ButtonVariant.PRIMARY);
        Button receiveBurst = new Button("Receive 3 in a burst",
                e -> receive(UI.getCurrent(), 3));
        Button markRead = new Button("Mark all as read",
                e -> markAllRead(UI.getCurrent()));

        Span counter = new Span();
        counter.addClassName("big-counter");
        counter.bindText(unread.map(n -> n + " unread"));

        log.addClassName("notification-log");
        add(new HorizontalLayout(allow, collapse),
                new HorizontalLayout(receiveOne, receiveBurst, markRead),
                counter, new H2("Notifications sent"), log);
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
        NotificationOptions options = NotificationOptions
                .body(sender + ": " + text);
        if (useTag) {
            options = options.withTag(TAG).withRenotify(true);
        }
        NotificationPermission permission = MissingAPI.permissionSignal(ui)
                .peek();
        if (permission == NotificationPermission.GRANTED) {
            MissingAPI.showNotification(ui, title, options);
            appendLog("\"" + title + "\"  tag="
                    + (useTag ? TAG : "(none)"));
        } else {
            appendLog("\"" + title + "\"  not shown — permission is "
                    + permission.name().toLowerCase());
        }
    }

    private void markAllRead(UI ui) {
        unread.set(0);
        MissingAPI.closeNotifications(ui, TAG);
        appendLog("marked all as read → closed tag=" + TAG);
    }

    private void appendLog(String line) {
        log.addComponentAsFirst(
                new Div(LocalTime.now().format(TIME) + "  " + line));
    }
}
