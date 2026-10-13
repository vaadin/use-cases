package com.example.desk;

import java.util.List;

import com.example.components.Address;
import com.example.components.Money;
import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.shared.SharedListSignal;

/**
 * The tickets, shared by every session; stands in for a database. A shared
 * signal, so that a ticket saved in one session shows up in every open service
 * desk at once.
 */
@Component
public class TicketStore {

    private final SharedListSignal<Ticket> tickets = new SharedListSignal<>(
            Ticket.class);

    public TicketStore() {
        reset();
    }

    /** The tickets, for views that follow changes. */
    public SharedListSignal<Ticket> tickets() {
        return tickets;
    }

    /** The tickets as they are now. */
    public List<Ticket> all() {
        return tickets.peekValues().toList();
    }

    public void save(Ticket ticket) {
        tickets.peek().stream().filter(t -> t.peek().id() == ticket.id())
                .findFirst().ifPresent(t -> t.set(ticket));
    }

    public void reset() {
        tickets.clear();
        tickets.insertLast(
                new Ticket(4521, "Espresso machine leaks", "Kestrel Air",
                        new Address("Hangar 3", "01530", "Vantaa", "Finland"),
                        List.of(42.0, 35.0, 30.0, 26.0, 18.0, 12.0, 9.0), 0,
                        null, "#dc2626", false));
        tickets.insertLast(
                new Ticket(4522, "Invoice shows wrong VAT", "Blue Finch",
                        new Address("Hauptstraße 5", "10115", "Berlin",
                                "Germany"),
                        List.of(12.0, 14.0, 11.0, 15.0, 13.0, 16.0, 18.0), 4,
                        Money.of("24.50", "EUR"), "#2563eb", false));
        tickets.insertLast(
                new Ticket(4523, "Grinder delivered late", "Harbour Foods",
                        new Address("Strandvägen 7", "114 56", "Stockholm",
                                "Sweden"),
                        List.of(30.0, 28.0, 25.0, 25.0, 20.0, 22.0, 21.0), 2,
                        null, "#16a34a", false));
    }
}
