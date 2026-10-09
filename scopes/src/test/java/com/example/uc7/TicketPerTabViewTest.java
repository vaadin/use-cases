package com.example.uc7;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers rendering and the per-ticket reply drafts. Keeping the ticket per
 * browser tab across reloads cannot be simulated in browserless tests yet; see
 * API-GAPS.md.
 */
@SpringBootTest
@ViewPackages(classes = TicketPerTabView.class)
class TicketPerTabViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersTicketPickerAndDisabledReply() {
        navigate(TicketPerTabView.class);

        assertEquals("UC7 — Ticket per tab",
                findInView(H1.class).single().getText());
        assertTrue(findInView(Span.class).withClassName("scope-badge").single()
                .getText().startsWith("Browser tab "));
        assertFalse(findInView(TextArea.class).single().isEnabled());
    }

    @Test
    @SuppressWarnings("unchecked")
    void replyDraftIsKeptPerTicket() {
        navigate(TicketPerTabView.class);
        Select<Ticket> ticket = findInView(Select.class).single();
        TextArea reply = findInView(TextArea.class).single();

        test(ticket).selectItem(Ticket.QUEUE.get(0).toString());
        test(reply).setValue("We have corrected the VAT rate.");

        test(ticket).selectItem(Ticket.QUEUE.get(1).toString());
        assertEquals("", reply.getValue());

        test(ticket).selectItem(Ticket.QUEUE.get(0).toString());
        assertEquals("We have corrected the VAT rate.", reply.getValue());
    }
}
