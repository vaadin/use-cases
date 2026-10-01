package com.example.uc7;

import java.util.List;

/**
 * A support ticket in the demo's fixed ticket queue.
 */
public record Ticket(String id, String customer, String subject) {

    public static final List<Ticket> QUEUE = List.of(
            new Ticket("T-101", "Nordic Bikes",
                    "Invoice shows the wrong VAT rate"),
            new Ticket("T-102", "Harbor Café", "Cannot reset password"),
            new Ticket("T-103", "Lumen Studio", "Export to Excel times out"));

    @Override
    public String toString() {
        return id + " — " + subject;
    }
}
