package com.example.uc6;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Last step of the UC6 booking wizard; see {@link SeatSelectionView}.
 */
@Route(value = "uc6/confirm", layout = MainLayout.class)
@PageTitle("UC6 — Booking wizard")
public class BookingSummaryView extends VerticalLayout {

    public BookingSummaryView(BookingDraft booking) {
        add(new H1("UC6 — Booking wizard"), new BookingSteps());
        add(new H2("3. Confirm"));

        Paragraph summary = new Paragraph(booking.isComplete()
                ? "Seat " + booking.getSeat() + " for " + booking.getPassenger()
                        + "."
                : "The booking is not complete yet: pick a seat and enter "
                        + "the passenger name.");
        Button confirm = new Button("Confirm booking", e -> {
            Notification.show("Booked seat " + booking.getSeat() + " for "
                    + booking.getPassenger() + ".");
            booking.confirm();
            summary.setText("Booking confirmed. Start a new one from step 1.");
            e.getSource().setEnabled(false);
        });
        confirm.addThemeVariants(ButtonVariant.PRIMARY);
        confirm.setEnabled(booking.isComplete());
        add(summary, confirm);
    }
}
