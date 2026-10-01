package com.example.uc7;

import java.util.Objects;

import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.BrowserTab;

/**
 * UC7 — A different ticket in each tab (browser tab scope vs. session scope).
 * <p>
 * A support agent works on several tickets at once, one per browser tab, and
 * types a reply in each. When they reload a tab, or the page reloads after a
 * redeploy, each tab must come back with its own ticket and its half-written
 * reply. With session scope, the last ticket opened in any tab wins and the
 * agent risks answering the wrong customer. {@link TicketWorkspace} is
 * browser-tab scoped; {@link LastOpenedTicket} shows the session-scoped
 * alternative next to it.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Ticket per tab")
@Menu(order = 7, title = "UC7 — Ticket per tab")
public class TicketPerTabView extends VerticalLayout {

    public TicketPerTabView(TicketWorkspace workspace,
            LastOpenedTicket lastOpened) {
        add(new H1("UC7 — Ticket per tab"));
        add(new Paragraph("Open a ticket here, open another one in a second "
                + "tab, start typing replies, then reload both tabs. Each "
                + "tab keeps its own ticket and reply."));

        Span tabId = new Span("Browser tab "
                + BrowserTab.get(UI.getCurrentOrThrow()).getId());
        tabId.addClassName("scope-badge");
        add(new HorizontalLayout(tabId, new Button("Reload this page",
                e -> UI.getCurrentOrThrow().getPage().reload())));

        Div sessionWarning = new Div();
        sessionWarning.addClassName("maintenance-banner");
        Ticket last = lastOpened.get();
        sessionWarning.setText("With session scope this tab would now show "
                + (last == null ? "no ticket" : last.id())
                + ", the last ticket opened in any tab.");
        sessionWarning
                .setVisible(!Objects.equals(last, workspace.getOpenTicket()));

        Select<Ticket> ticket = new Select<>();
        ticket.setLabel("Ticket");
        ticket.setItems(Ticket.QUEUE);
        ticket.setWidth("28rem");
        // Restore before adding the listener so the session-scoped copy is
        // not overwritten on reload.
        ticket.setValue(workspace.getOpenTicket());

        TextArea reply = new TextArea("Reply");
        reply.setWidth("28rem");
        reply.setValueChangeMode(ValueChangeMode.EAGER);
        reply.addValueChangeListener(e -> {
            Ticket open = workspace.getOpenTicket();
            if (open != null && e.isFromClient()) {
                workspace.setReplyDraft(open, e.getValue());
            }
        });

        ticket.addValueChangeListener(e -> {
            workspace.open(e.getValue());
            lastOpened.set(e.getValue());
            sessionWarning.setVisible(false);
            showTicket(workspace, reply);
        });

        showTicket(workspace, reply);

        add(sessionWarning, ticket, reply);
    }

    private static void showTicket(TicketWorkspace workspace, TextArea reply) {
        Ticket open = workspace.getOpenTicket();
        reply.setEnabled(open != null);
        reply.setValue(open == null ? "" : workspace.getReplyDraft(open));
        reply.setHelperText(open == null ? "Pick a ticket first."
                : "Replying to " + open.customer());
    }
}
