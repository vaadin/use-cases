package com.example.uc7;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * The same "open ticket" state, kept in session scope for comparison. Every tab
 * of the user writes to this one instance, so after a reload a tab would show
 * whatever ticket was opened last in any tab.
 */
@SpringComponent
@VaadinSessionScope
public class LastOpenedTicket {

    private @Nullable Ticket ticket;

    public @Nullable Ticket get() {
        return ticket;
    }

    public void set(@Nullable Ticket ticket) {
        this.ticket = ticket;
    }
}
