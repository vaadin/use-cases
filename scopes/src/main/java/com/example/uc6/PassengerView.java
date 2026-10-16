package com.example.uc6;

import com.example.views.MainLayout;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Second step of the UC6 booking wizard; see {@link SeatSelectionView}.
 */
@Route(value = "uc6/passenger", layout = MainLayout.class)
@PageTitle("UC6 — Booking wizard")
public class PassengerView extends VerticalLayout {

    public PassengerView(BookingDraft booking) {
        add(new H1("UC6 — Booking wizard"), new BookingSteps());
        add(new H2("2. Who is coming?"));

        String seat = booking.getSeat();
        add(new Span(seat == null ? "No seat picked yet."
                : "Seat " + seat + " is held for you."));

        TextField passenger = new TextField("Passenger name",
                booking.getPassenger(), "");
        passenger.setValueChangeMode(ValueChangeMode.EAGER);
        passenger.addValueChangeListener(
                e -> booking.setPassenger(e.getValue()));
        add(passenger);
    }
}
