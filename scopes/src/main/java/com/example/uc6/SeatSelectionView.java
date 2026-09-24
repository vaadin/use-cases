package com.example.uc6;

import java.util.Map;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC6 — Booking wizard that survives reloads (browser tab scope).
 * <p>
 * A theatre-goer books a seat in three steps, each on its own page. They reload
 * a page, or use the browser's back button, and expect the booking to still be
 * there. Meanwhile they may start a second booking for a friend in another tab,
 * which must not overwrite the first one. When they abandon a booking by
 * closing the tab, the held seat has to go back on sale.
 * <p>
 * UI scope loses the booking on reload, and session scope mixes up the two
 * tabs. {@link BookingDraft} is therefore browser-tab scoped.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Booking wizard")
@Menu(order = 6, title = "UC6 — Booking wizard")
public class SeatSelectionView extends VerticalLayout {

    public SeatSelectionView(BookingDraft booking, SeatInventory inventory) {
        add(new H1("UC6 — Booking wizard"));
        add(new Paragraph("Your booking is kept per browser tab: reload or "
                + "move between the steps and it stays. Open the wizard in "
                + "a second tab to book another seat — the two bookings "
                + "stay separate. Close a tab and its held seat is "
                + "released after the heartbeat times out."));
        add(new BookingSteps());
        add(new H2("1. Pick a seat"));

        Signal<Map<String, String>> holders = inventory.holders();
        HorizontalLayout seats = new HorizontalLayout();
        seats.setWrap(true);
        for (String seat : SeatInventory.SEATS) {
            Button button = new Button(seat, e -> {
                if (!booking.selectSeat(seat)) {
                    Notification.show("Seat " + seat + " was just taken.");
                }
            });
            Signal<String> holder = holders
                    .map(map -> map.getOrDefault(seat, ""));
            button.bindEnabled(holder
                    .map(h -> h.isEmpty() || h.equals(booking.getHolderId())));
            button.bindThemeName(ButtonVariant.PRIMARY.getVariantName(),
                    holder.map(h -> h.equals(booking.getHolderId())));
            seats.add(button);
        }
        add(seats);
    }
}
