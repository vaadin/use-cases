package com.example.uc6;

import jakarta.annotation.PreDestroy;

import java.util.UUID;

import com.example.MissingAPI.BrowserTabScope;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.spring.annotation.SpringComponent;

/**
 * The half-finished booking in one browser tab.
 * <p>
 * {@code @BrowserTabScope} (from {@link com.example.MissingAPI}) keeps the
 * booking for as long as the browser tab stays open, across page reloads and
 * full page navigation between the wizard steps. A second tab gets its own
 * booking. When the tab is closed and its heartbeat times out, Spring calls
 * {@link #releaseHold()} and the held seat goes back on sale.
 */
@SpringComponent
@BrowserTabScope
public class BookingDraft {

    private final SeatInventory inventory;
    private final String holderId = UUID.randomUUID().toString();

    private @Nullable String seat;
    private String passenger = "";

    public BookingDraft(SeatInventory inventory) {
        this.inventory = inventory;
    }

    public String getHolderId() {
        return holderId;
    }

    public @Nullable String getSeat() {
        return seat;
    }

    public boolean selectSeat(String newSeat) {
        if (!inventory.hold(newSeat, holderId)) {
            return false;
        }
        if (seat != null && !seat.equals(newSeat)) {
            inventory.release(seat, holderId);
        }
        seat = newSeat;
        return true;
    }

    public String getPassenger() {
        return passenger;
    }

    public void setPassenger(String passenger) {
        this.passenger = passenger;
    }

    public boolean isComplete() {
        return seat != null && !passenger.isBlank();
    }

    /**
     * Sells the held seat and starts over with an empty booking.
     */
    public void confirm() {
        if (seat != null) {
            inventory.sell(seat, holderId);
        }
        seat = null;
        passenger = "";
    }

    @PreDestroy
    public void releaseHold() {
        if (seat != null) {
            inventory.release(seat, holderId);
            seat = null;
        }
    }
}
