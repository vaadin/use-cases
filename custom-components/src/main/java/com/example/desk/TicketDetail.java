package com.example.desk;

import java.util.Objects;

import com.example.components.AddressForm;
import com.example.components.ColorPicker;
import com.example.components.MoneyField;
import com.example.components.QrCode;
import com.example.components.RatingStars;
import com.example.components.SignaturePad;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;

/**
 * The detail of one ticket (UC10), itself a component made of the other
 * components: the customer's address (read-only), a refund, the customer's
 * rating, a label color, a tracking QR code and the customer's signature to
 * close the ticket.
 * <p>
 * It knows nothing about the list next to it or the store: it shows the ticket
 * it is given and reports changes with {@link TicketSavedEvent}.
 */
public class TicketDetail extends Composite<VerticalLayout> {

    /** Fired when the user saves or closes the ticket. */
    public static class TicketSavedEvent extends ComponentEvent<TicketDetail> {

        private final Ticket ticket;

        public TicketSavedEvent(TicketDetail source, Ticket ticket) {
            super(source, true);
            this.ticket = ticket;
        }

        public Ticket getTicket() {
            return ticket;
        }
    }

    private final H2 title = new H2();
    private final AddressForm address = new AddressForm();
    private final MoneyField refund = new MoneyField("Refund");
    private final RatingStars rating = new RatingStars("Customer rating");
    private final ColorPicker label = new ColorPicker();
    private final QrCode tracking = new QrCode("");
    private final SignaturePad signature = new SignaturePad();
    private final Button save = new Button("Save");
    private final Button close = new Button("Close ticket");
    private final Span status = new Span();
    private @Nullable Ticket ticket;

    public TicketDetail() {
        address.setReadOnly(true);
        signature.setWidth("18rem");
        signature.setHeight("6rem");
        tracking.setModuleSize(3);
        save.addThemeVariants(ButtonVariant.PRIMARY);
        status.addClassName("status");
        close.setEnabled(false);
        signature.addValueChangeListener(e -> close.setEnabled(
                ticket != null && !ticket.closed() && !e.getValue().isEmpty()));
        save.addClickListener(e -> fireSaved(false));
        close.addClickListener(e -> fireSaved(true));
        setTicket(null);
    }

    @Override
    protected VerticalLayout initContent() {
        HorizontalLayout labelRow = new HorizontalLayout(label, tracking);
        labelRow.setAlignItems(Alignment.START);
        VerticalLayout layout = new VerticalLayout(title, new H3("Customer"),
                address, refund, rating, new H3("Label and tracking"), labelRow,
                new H3("Customer signature"), signature,
                new HorizontalLayout(save, close), status);
        layout.setPadding(false);
        return layout;
    }

    public void setTicket(@Nullable Ticket ticket) {
        this.ticket = ticket;
        getContent().setVisible(ticket != null);
        if (ticket == null) {
            return;
        }
        title.setText("#" + ticket.id() + " " + ticket.title());
        address.setAddress(ticket.address());
        refund.setValue(ticket.refund());
        rating.setValue(ticket.rating());
        label.setColor(ticket.labelColor());
        tracking.setData(ticket.trackingLink());
        signature.clear();
        signature.setEnabled(!ticket.closed());
        save.setEnabled(!ticket.closed());
        close.setEnabled(false);
        status.setText(ticket.closed() ? "Closed" : "Open");
    }

    private void fireSaved(boolean closing) {
        if (ticket == null) {
            return;
        }
        Ticket saved = ticket.withEdits(rating.getValue(), refund.getValue(),
                label.getColor(), closing);
        setTicket(saved);
        fireEvent(new TicketSavedEvent(this, saved));
    }

    /** Whether this detail last showed or saved exactly this ticket. */
    public boolean isShowing(@Nullable Ticket other) {
        return Objects.equals(ticket, other);
    }

    public Registration addTicketSavedListener(
            ComponentEventListener<TicketSavedEvent> listener) {
        return addListener(TicketSavedEvent.class, listener);
    }

    // Test seams, public because the view's test lives in another package.
    public RatingStars rating() {
        return rating;
    }

    public MoneyField refund() {
        return refund;
    }

    public SignaturePad signature() {
        return signature;
    }

    public Button saveButton() {
        return save;
    }

    public Button closeButton() {
        return close;
    }

    public QrCode tracking() {
        return tracking;
    }
}
