package com.example.uc10;

import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * UC10's shared state: one note that several users edit.
 * <p>
 * A plain {@link SharedValueSignal}, the same type a last-write-wins form would
 * use. What makes the edits safe is not the type but how the view writes to it:
 * with {@code replace(expected, new)}, a compare-and-set, instead of
 * {@code set(new)}.
 */
@Component
public class RaceTopic {

    private final SharedValueSignal<String> note = new SharedValueSignal<>(
            "Order #1041 — waiting for stock");

    public SharedValueSignal<String> note() {
        return note;
    }
}
