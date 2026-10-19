package com.example.uc6;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedMapSignal;

/**
 * Seats of the one evening show everybody books from. Application-scoped
 * because all users compete for the same seats. A seat is either free, held by
 * one unfinished booking (identified by a holder id), or sold.
 */
@Component
public class SeatInventory {

    public static final List<String> SEATS = List.of("A1", "A2", "A3", "A4",
            "B1", "B2", "B3", "B4");

    static final String SOLD = "sold";

    // The source of truth for hold, release and sell. A write to the shared
    // signal from a UI request is only applied when the session is flushed,
    // so reading the signal back could let two users hold the same seat.
    private final Map<String, String> state = new HashMap<>();

    private final SharedMapSignal<String> holders = new SharedMapSignal<>(
            String.class);

    /**
     * Seat to holder id (or {@value #SOLD}); free seats are absent.
     */
    public Signal<Map<String, String>> holders() {
        return Signal.computed(() -> holders.get().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().get())));
    }

    public synchronized boolean hold(String seat, String holder) {
        String current = state.putIfAbsent(seat, holder);
        if (current != null && !current.equals(holder)) {
            return false;
        }
        holders.put(seat, holder);
        return true;
    }

    public synchronized void release(String seat, String holder) {
        if (state.remove(seat, holder)) {
            holders.remove(seat);
        }
    }

    public synchronized void sell(String seat, String holder) {
        if (state.replace(seat, holder, SOLD)) {
            holders.put(seat, SOLD);
        }
    }
}
