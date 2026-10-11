package com.example.desk;

import java.util.ArrayList;
import java.util.List;

import com.example.components.Address;
import com.example.components.Money;
import org.springframework.stereotype.Component;

/** The tickets, shared by every session; stands in for a database. */
@Component
public class TicketStore {

    private final List<Ticket> tickets = new ArrayList<>();

    public TicketStore() {
        reset();
    }

    public synchronized List<Ticket> all() {
        return List.copyOf(tickets);
    }

    public synchronized void save(Ticket ticket) {
        tickets.replaceAll(t -> t.id() == ticket.id() ? ticket : t);
    }

    public synchronized void reset() {
        tickets.clear();
        tickets.add(new Ticket(4521, "Espresso machine leaks", "Kestrel Air",
                new Address("Hangar 3", "01530", "Vantaa", "Finland"),
                List.of(42.0, 35.0, 30.0, 26.0, 18.0, 12.0, 9.0), 0, null,
                "#dc2626", false));
        tickets.add(new Ticket(4522, "Invoice shows wrong VAT", "Blue Finch",
                new Address("Hauptstraße 5", "10115", "Berlin", "Germany"),
                List.of(12.0, 14.0, 11.0, 15.0, 13.0, 16.0, 18.0), 4,
                Money.of("24.50", "EUR"), "#2563eb", false));
        tickets.add(new Ticket(4523, "Grinder delivered late", "Harbour Foods",
                new Address("Strandvägen 7", "114 56", "Stockholm", "Sweden"),
                List.of(30.0, 28.0, 25.0, 25.0, 20.0, 22.0, 21.0), 2, null,
                "#16a34a", false));
    }
}
