package com.example.uc3;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.MissingAPI;
import com.example.MissingAPI.NotificationOptions;
import com.example.MissingAPI.NotificationPermission;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC3 — Click a notification to open the related item.
 * <p>
 * A support agent works in another tab while new tickets are assigned to them.
 * Each assignment raises a native notification. Clicking it brings the
 * browser tab to the front and opens the ticket it refers to — the click is
 * delivered to the server, which selects the ticket and shows its details.
 * The notification uses {@code requireInteraction} so it stays on screen until
 * the agent acts on it. Without permission, an in-app notification with an
 * "Open" button does the same job.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Click to open")
@Menu(order = 3, title = "UC3 — Click to open")
public class ClickToOpenView extends VerticalLayout {

    static final Duration DELAY = Duration.ofSeconds(5);
    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    record Ticket(int number, String customer, String subject) {
        String label() {
            return "#" + number + " — " + subject + " (" + customer + ")";
        }
    }

    private static final List<Ticket> INCOMING = List.of(
            new Ticket(0, "Acme Corp", "Invoice PDF is blank"),
            new Ticket(0, "Globex", "Cannot reset password"),
            new Ticket(0, "Initech", "Export to Excel times out"),
            new Ticket(0, "Umbrella", "Dark mode colours unreadable"));

    private final TaskScheduler taskScheduler;
    private final AtomicInteger nextNumber = new AtomicInteger(1041);

    private final ListSignal<Ticket> tickets = new ListSignal<>();
    private final ValueSignal<@Nullable Ticket> selected = new ValueSignal<>(
            null);
    private final ValueSignal<String> openedHow = new ValueSignal<>("");

    private final Div ticketList = new Div();
    private final Div details = new Div();

    public ClickToOpenView(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;

        add(new H1("UC3 — Click a notification to open the related item"));
        add(new Paragraph("Enable notifications, click \"Assign me a "
                + "ticket\" and switch to another tab. After five seconds "
                + "a notification appears; clicking it brings this tab "
                + "back and opens the ticket."));

        Button allow = new Button("Allow notifications");
        MissingAPI.requestPermissionOnClick(allow);
        Button assign = new Button("Assign me a ticket in 5 seconds",
                e -> scheduleAssignment(UI.getCurrent()));
        assign.addThemeVariants(ButtonVariant.PRIMARY);
        add(new HorizontalLayout(allow, assign));

        ticketList.addClassName("ticket-list");
        add(new H2("My tickets"), ticketList, new H2("Opened ticket"),
                details);

        Signal.effect(this, () -> {
            ticketList.removeAll();
            Ticket current = selected.get();
            List<Ticket> all = tickets.getValues().toList();
            if (all.isEmpty()) {
                ticketList.add(new Span("(no tickets assigned yet)"));
            }
            for (Ticket ticket : all) {
                Div row = new Div(ticket.label());
                if (ticket.equals(current)) {
                    row.addClassName("selected");
                }
                ticketList.add(row);
            }
        });
        Signal.effect(this, () -> {
            details.removeAll();
            Ticket current = selected.get();
            if (current == null) {
                details.add(new Span("Nothing opened yet."));
                return;
            }
            details.add(new H3("Ticket #" + current.number()),
                    new Paragraph(current.subject()),
                    new Paragraph("Customer: " + current.customer()),
                    new Span(openedHow.get()));
        });

        // Button disabled only while the browser hasn't reported yet or once
        // the user has decided.
        Signal<NotificationPermission> permission = MissingAPI
                .permissionSignal(UI.getCurrent());
        allow.bindEnabled(
                permission.map(p -> p == NotificationPermission.DEFAULT));
    }

    private void scheduleAssignment(UI ui) {
        Notification.show("A ticket will be assigned in 5 seconds — switch "
                + "tabs now", 2500, Notification.Position.BOTTOM_START);
        taskScheduler.schedule(ui.accessLater(() -> assignTicket(ui), null),
                Instant.now().plus(DELAY));
    }

    // Package-private so tests can assign a ticket without waiting.
    Ticket assignTicket(UI ui) {
        int number = nextNumber.getAndIncrement();
        Ticket template = INCOMING.get(number % INCOMING.size());
        Ticket ticket = new Ticket(number, template.customer(),
                template.subject());
        tickets.insertLast(ticket);

        if (MissingAPI.permissionSignal(ui)
                .peek() == NotificationPermission.GRANTED) {
            MissingAPI
                    .showNotification(ui,
                            "Ticket #" + number + " assigned to you",
                            NotificationOptions
                                    .body(ticket.subject() + " — "
                                            + ticket.customer())
                                    .withRequireInteraction(true))
                    .onClick(n -> open(ticket, "notification click"));
        } else {
            Notification toast = new Notification();
            toast.setDuration(8000);
            toast.setPosition(Notification.Position.BOTTOM_END);
            Button openButton = new Button("Open", e -> {
                open(ticket, "in-app notification");
                toast.close();
            });
            openButton.addThemeVariants(ButtonVariant.TERTIARY);
            HorizontalLayout content = new HorizontalLayout(
                    new Span("Ticket #" + number + " assigned to you"),
                    openButton);
            content.setAlignItems(Alignment.CENTER);
            toast.add(content);
            toast.open();
        }
        return ticket;
    }

    private void open(Ticket ticket, String how) {
        openedHow.set("Opened from " + how + " at "
                + LocalTime.now().format(TIME));
        selected.set(ticket);
    }
}
