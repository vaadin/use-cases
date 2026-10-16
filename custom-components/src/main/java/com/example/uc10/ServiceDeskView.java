package com.example.uc10;

import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.components.RatingStars;
import com.example.components.RatingStarsVariant;
import com.example.components.Sparkline;
import com.example.desk.Ticket;
import com.example.desk.TicketDetail;
import com.example.desk.TicketStore;
import com.example.views.BuiltWith;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC10 — The composite app.
 * <p>
 * A service desk built from the components of UC1–UC7: a ticket list whose rows
 * show a read-only {@link RatingStars} and a {@link Sparkline} of response
 * times, and a {@link TicketDetail} that is itself a component made of the
 * address form, the money field, the rating, the color picker, the QR code and
 * the signature pad.
 * <p>
 * The pieces talk through their APIs only: selecting a row hands the ticket to
 * the detail, and the detail's {@link TicketDetail.TicketSavedEvent} is what
 * makes the view store it and refresh the list.
 */
@Route(value = "uc10", layout = MainLayout.class)
@PageTitle("UC10 — The composite app")
@UseCaseDescription("Assembling a screen from custom components that talk through events")
@Menu(order = 10, title = "UC10 — The composite app")
public class ServiceDeskView extends VerticalLayout {

    static final int NARROW = 900;

    private final Grid<Ticket> grid = new Grid<>();
    private final SplitLayout split = new SplitLayout();
    private final TicketDetail detail = new TicketDetail();
    private boolean refreshing;

    public ServiceDeskView(TicketStore store) {
        setSizeFull();
        add(new H1("UC10 — The composite app"));
        add(new Paragraph("Pick a ticket, change the refund, the rating or "
                + "the label, and save. Let the customer sign to close it."));

        grid.addColumn(Ticket::id).setHeader("#").setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(Ticket::title).setHeader("Ticket");
        grid.addColumn(Ticket::customer).setHeader("Customer");
        grid.addComponentColumn(ticket -> {
            RatingStars stars = new RatingStars(
                    "Rating of ticket " + ticket.id());
            stars.setValue(ticket.rating());
            stars.setReadOnly(true);
            stars.addThemeVariants(RatingStarsVariant.SMALL,
                    RatingStarsVariant.READ_ONLY_COMPACT);
            return stars;
        }).setHeader("Rating").setKey("rating").setAutoWidth(true);
        grid.addComponentColumn(
                ticket -> new Sparkline(ticket.responseMinutes(),
                        "First response times of ticket " + ticket.id()))
                .setHeader("Response").setKey("trend").setAutoWidth(true);
        grid.addColumn(ticket -> ticket.closed() ? "Closed" : "Open")
                .setHeader("Status").setKey("status");
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (!refreshing) {
                detail.setTicket(e.getValue());
            }
        });
        // The list follows the shared store, so a ticket saved in another
        // session shows up here at once; the selected row stays selected.
        Signal.effect(this, () -> {
            List<Ticket> tickets = store.tickets().getValues().toList();
            Ticket selected = grid.asSingleSelect().getValue();
            refreshing = true;
            grid.setItems(tickets);
            Ticket current = selected == null ? null
                    : tickets.stream().filter(t -> t.id() == selected.id())
                            .findFirst().orElse(null);
            if (current != null) {
                grid.select(current);
            }
            refreshing = false;
            // Changed or removed in another session (this detail's own save
            // already shows the new ticket): show what is stored now, so a
            // later save here does not quietly undo that change.
            if (selected != null && !selected.equals(current)
                    && !detail.isShowing(current)) {
                detail.setTicket(current);
                if (current != null) {
                    Notification.show("Ticket #" + current.id()
                            + " was changed in another session");
                }
            }
        });

        detail.addTicketSavedListener(e -> {
            store.save(e.getTicket());
            Notification.show("Ticket #" + e.getTicket().id()
                    + (e.getTicket().closed() ? " closed" : " saved"));
        });

        Div detailBox = new Div(detail);
        detailBox.getStyle().set("padding", "0 1rem");
        split.addToPrimary(grid);
        split.addToSecondary(detailBox);
        split.setSplitterPosition(55);
        split.setSizeFull();
        // Side by side on wide screens, list above detail on phones.
        Signal.effect(this, () -> {
            boolean narrow = UI.getCurrent().getPage().windowSizeSignal().get()
                    .width() < NARROW;
            split.setOrientation(narrow ? SplitLayout.Orientation.VERTICAL
                    : SplitLayout.Orientation.HORIZONTAL);
            split.setSplitterPosition(narrow ? 45 : 55);
        });
        add(split);
        add(new BuiltWith("TicketDetail", "A component made of components")
                .point("TicketDetail is a Composite of the address form, money field, rating, color picker, QR code and signature pad.")
                .point("It knows nothing about the list or the store: it shows the ticket it is given and fires TicketSavedEvent.")
                .point("The view only wires the two: row selection calls setTicket, and the event saves and refreshes the list.")
                .java("desk/TicketDetail").java("desk/Ticket")
                .java("desk/TicketStore"));
    }

    // Package-private test seams.
    Grid<Ticket> grid() {
        return grid;
    }

    TicketDetail detail() {
        return detail;
    }

    SplitLayout split() {
        return split;
    }
}
