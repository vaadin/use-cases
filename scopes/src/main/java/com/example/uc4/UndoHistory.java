package com.example.uc4;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * Undo history of the editor in one UI.
 * <p>
 * {@code @UIScope} gives one instance per {@code UI}: every component that is
 * created for the same UI — here the editor and the separately injected
 * {@link UndoToolbar} — gets the same history without passing it around. A
 * second tab is a second UI and has its own history. A reload also creates a
 * new UI, so the history starts over, which is fine for undo but makes UI scope
 * the wrong choice for anything the user expects to survive a reload. A local
 * {@link ValueSignal} is enough because only one UI ever touches it.
 */
@SpringComponent
@UIScope
public class UndoHistory {

    public record Entry(String description, SerializableRunnable undo) {
    }

    private final ValueSignal<List<Entry>> entries = new ValueSignal<>(
            List.of());

    public Signal<List<Entry>> entries() {
        return entries.asReadonly();
    }

    public void record(String description, SerializableRunnable undo) {
        entries.update(list -> {
            List<Entry> copy = new ArrayList<>(list);
            copy.add(new Entry(description, undo));
            return List.copyOf(copy);
        });
    }

    public void undo() {
        List<Entry> list = entries.peek();
        if (list.isEmpty()) {
            return;
        }
        Entry last = list.getLast();
        entries.set(List.copyOf(list.subList(0, list.size() - 1)));
        last.undo().run();
    }
}
