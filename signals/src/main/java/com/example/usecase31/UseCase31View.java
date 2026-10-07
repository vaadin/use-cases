package com.example.usecase31;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Workshop seat assignment for an in-person event. Eight numbered seats can be
 * claimed by attendees. When two devices try to claim the same seat at the same
 * instant (a likely scenario when attendees scan a QR code on arrival), the
 * {@code replace(null, name)} compare-and-set ensures exactly one wins; the
 * other sees "already taken".
 * <p>
 * A separate "reservations today" counter is incremented from multiple sources
 * (admins, the QR endpoint, this view). {@code update(old -> old + 1)} runs the
 * increment atomically under the signal lock so concurrent bumps from different
 * threads don't lose updates.
 */
@PageTitle("Use Case 31: Seat reservation")
@Route(value = "use-case-31", layout = MainLayout.class)
@Menu(order = 31, title = "UC 31: Seat reservation")
@AnonymousAllowed
public class UseCase31View extends VerticalLayout {

    private static final int SEAT_COUNT = 8;

    final List<ValueSignal<@Nullable String>> seats = new ArrayList<>();
    final ValueSignal<Integer> reservationCount = new ValueSignal<>(0);

    public UseCase31View() {
        setSpacing(true);
        setPadding(true);

        for (int i = 0; i < SEAT_COUNT; i++) {
            seats.add(new ValueSignal<@Nullable String>(null));
        }

        add(new H2("Use Case 31: Workshop seat assignment"),
                new Paragraph(
                        "Each seat is a writable signal. Claims and releases"
                                + " use compare-and-set (replace), so two"
                                + " devices racing for the same seat at the"
                                + " same moment still produce exactly one"
                                + " winner. The 'reservations today' counter"
                                + " is shared across admin tools and"
                                + " incremented with update(old -> old + 1)"
                                + " for race-free counting."));

        TextField nameField = new TextField("Your name");
        nameField.setValue("Attendee A");
        nameField.setWidth("220px");

        add(nameField, buildSeatGrid(nameField), buildContendedClaim(),
                buildReservationCounter(), buildExplanation());
    }

    private Div buildSeatGrid(TextField nameField) {
        Div grid = new Div();
        grid.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(4, 1fr)")
                .set("gap", "var(--lumo-space-s)");

        for (int i = 0; i < SEAT_COUNT; i++) {
            int seatNumber = i + 1;
            ValueSignal<@Nullable String> seat = seats.get(i);
            grid.add(buildSeatTile(seatNumber, seat, nameField));
        }
        return grid;
    }

    private Div buildSeatTile(int seatNumber,
            ValueSignal<@Nullable String> seat, TextField nameField) {
        Div tile = new Div();
        tile.getStyle().set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px").set("text-align", "center")
                .set("cursor", "pointer")
                .set("border", "1px solid var(--lumo-contrast-20pct)");
        tile.getStyle().bind("background-color",
                seat.map(occupant -> occupant == null
                        ? "var(--lumo-success-color-10pct)"
                        : "var(--lumo-error-color-10pct)"));

        Span title = new Span("Seat " + seatNumber);
        title.getStyle().set("display", "block").set("font-weight", "bold");
        Span occupant = new Span();
        occupant.bindText(seat.map(o -> o == null ? "Available" : o));
        occupant.getStyle().set("display", "block").set("color",
                "var(--lumo-secondary-text-color)");

        tile.add(title, occupant);
        tile.addClickListener(e -> {
            String name = nameField.getValue();
            String current = seat.peek();
            if (current == null) {
                if (name == null || name.isBlank()) {
                    Notification.show("Type your name first");
                    return;
                }
                if (seat.replace(null, name)) {
                    reservationCount.update(n -> n + 1);
                    Notification.show("Claimed seat " + seatNumber);
                } else {
                    Notification.show(
                            "Sorry — seat " + seatNumber + " was just taken");
                }
            } else if (current.equals(name)) {
                seat.replace(name, null);
                Notification.show("Released seat " + seatNumber);
            } else {
                Notification
                        .show("Seat " + seatNumber + " is held by " + current);
            }
        });
        return tile;
    }

    private Div buildContendedClaim() {
        Div section = new Div();
        section.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px");

        H3 title = new H3("Simulate 4 devices racing to claim seat 1");
        title.getStyle().set("margin-top", "0");

        ValueSignal<Integer> winners = new ValueSignal<>(0);
        Span result = new Span();
        result.bindText(winners
                .map(n -> "Last race result: " + n + " device claimed seat 1"));

        Button race = new Button("Reset seat 1 and have 4 devices try at once",
                e -> {
                    ValueSignal<@Nullable String> seat = seats.get(0);
                    seat.set(null);

                    AtomicInteger won = new AtomicInteger();
                    CountDownLatch start = new CountDownLatch(1);
                    List<Thread> threads = new ArrayList<>();
                    for (int i = 0; i < 4; i++) {
                        String attendee = "Device-" + i;
                        threads.add(Thread.ofVirtual().start(() -> {
                            try {
                                start.await();
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                            if (seat.replace(null, attendee)) {
                                won.incrementAndGet();
                                reservationCount.update(n -> n + 1);
                            }
                        }));
                    }
                    start.countDown();
                    for (Thread t : threads) {
                        try {
                            t.join();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    winners.set(won.get());
                });

        section.add(title, race, result);
        return section;
    }

    private Div buildReservationCounter() {
        Div section = new Div();
        section.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px");

        H3 title = new H3("Reservations today");
        title.getStyle().set("margin-top", "0");

        Span value = new Span();
        value.bindText(reservationCount.map(n -> "Total: " + n));
        value.getStyle().set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "bold")
                .set("margin-right", "var(--lumo-space-m)");

        Button burst = new Button(
                "Simulate 6 admin tools registering walk-ins at once", e -> {
                    CountDownLatch start = new CountDownLatch(1);
                    List<Thread> threads = new ArrayList<>();
                    for (int i = 0; i < 6; i++) {
                        threads.add(Thread.ofVirtual().start(() -> {
                            try {
                                start.await();
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                            reservationCount.update(n -> n + 1);
                        }));
                    }
                    start.countDown();
                    for (Thread t : threads) {
                        try {
                            t.join();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });

        HorizontalLayout row = new HorizontalLayout(value, burst);
        row.setAlignItems(
                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.BASELINE);
        section.add(title, row);
        return section;
    }

    private Div buildExplanation() {
        Div box = new Div();
        box.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px");

        H3 title = new H3("Why CAS + update?");
        title.getStyle().set("margin-top", "0");

        Paragraph p = new Paragraph(
                "replace(null, name) is the signal-native compare-and-set:"
                        + " it only writes if the seat is still empty,"
                        + " so two simultaneous claims can't both succeed."
                        + " update(old -> old + 1) takes the signal lock"
                        + " for the duration of the increment, keeping the"
                        + " reservation counter consistent even when several"
                        + " admin tools are bumping it from different"
                        + " threads.");

        box.add(title, p);
        return box;
    }
}
