package com.example.uc10;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedNodeSignal;
import com.vaadin.flow.signals.shared.SharedNumberSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * UC10's shared state, one structure per way of losing a race.
 * <ul>
 * <li>{@link #note} — a plain shared value, written both blindly and with a
 * compare-and-set, which is the difference the use case is about.</li>
 * <li>{@link #tally} — a number, incremented rather than set, so no concurrent
 * increment can be lost.</li>
 * <li>{@link #backlog} and {@link #done} — two lists an item moves between,
 * which is only correct if both changes land together.</li>
 * </ul>
 * <p>
 * The two lists are children of one {@link SharedNodeSignal} rather than two
 * separate signals, and that is not a stylistic choice: a transaction cannot
 * span independent shared signals, because each one commits on its own and may
 * live on a different cluster node. Two top-level lists here would make the
 * move in {@code ConflictsView} throw. Keeping them in one tree is what the
 * framework's own error message recommends, and it is the constraint any
 * application that wants atomic multi-collection changes has to design around
 * from the start.
 */
@Component
public class RaceTopic {

    private final SharedValueSignal<String> note = new SharedValueSignal<>(
            "Order #1041 — waiting for stock");

    private final SharedNumberSignal tally = new SharedNumberSignal(0);

    /** Holds both lists, so that an item can move between them atomically. */
    private final SharedNodeSignal board = new SharedNodeSignal();

    public SharedValueSignal<String> note() {
        return note;
    }

    public SharedNumberSignal tally() {
        return tally;
    }

    public SharedListSignal<String> backlog() {
        return list("backlog");
    }

    public SharedListSignal<String> done() {
        return list("done");
    }

    private SharedListSignal<String> list(String key) {
        SharedNodeSignal child = board.peek().mapChildren().get(key);
        if (child == null) {
            board.putChildIfAbsent(key);
            child = board.peek().mapChildren().get(key);
        }
        if (child == null) {
            throw new IllegalStateException("List " + key + " was not created");
        }
        return child.asList(String.class);
    }

    @PostConstruct
    void seed() {
        if (backlog().peek().isEmpty()) {
            backlog().insertAllLast(java.util.List.of("Pick order #1041",
                    "Pack order #1041", "Label order #1041"));
        }
    }
}
