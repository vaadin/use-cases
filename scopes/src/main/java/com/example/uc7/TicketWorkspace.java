package com.example.uc7;

import java.util.HashMap;
import java.util.Map;

import com.example.MissingAPI.BrowserTabScope;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.spring.annotation.SpringComponent;

/**
 * What the agent is working on in one browser tab: the open ticket and the
 * reply drafts typed so far. Browser-tab scoped, so each tab has its own
 * workspace and a reload keeps it.
 */
@SpringComponent
@BrowserTabScope
public class TicketWorkspace {

    private @Nullable Ticket openTicket;
    private final Map<String, String> replyDrafts = new HashMap<>();

    public @Nullable Ticket getOpenTicket() {
        return openTicket;
    }

    public void open(@Nullable Ticket ticket) {
        openTicket = ticket;
    }

    public String getReplyDraft(Ticket ticket) {
        return replyDrafts.getOrDefault(ticket.id(), "");
    }

    public void setReplyDraft(Ticket ticket, String draft) {
        replyDrafts.put(ticket.id(), draft);
    }
}
