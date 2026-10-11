package com.example.uc10;

import com.example.common.UseCaseDescription;
import com.example.components.RatingStars;
import com.example.components.RatingStarsVariant;
import com.example.components.Sparkline;
import com.example.desk.Ticket;
import com.example.desk.TicketDetail;
import com.example.desk.TicketStore;
import com.example.views.MainLayout;

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

    private final Grid<Ticket> grid = new Grid<>();
    private final TicketDetail detail = new TicketDetail();

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
        grid.setItems(store.all());
        grid.asSingleSelect()
                .addValueChangeListener(e -> detail.setTicket(e.getValue()));

        detail.addTicketSavedListener(e -> {
            store.save(e.getTicket());
            grid.setItems(store.all());
            Notification.show("Ticket #" + e.getTicket().id()
                    + (e.getTicket().closed() ? " closed" : " saved"));
        });

        Div detailBox = new Div(detail);
        detailBox.getStyle().set("padding", "0 1rem");
        SplitLayout split = new SplitLayout(grid, detailBox);
        split.setSplitterPosition(55);
        split.setSizeFull();
        add(split);
    }

    // Package-private test seams.
    Grid<Ticket> grid() {
        return grid;
    }

    TicketDetail detail() {
        return detail;
    }
}
