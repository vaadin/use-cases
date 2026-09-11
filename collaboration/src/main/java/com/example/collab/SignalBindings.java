package com.example.collab;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * Bindings this module needs and the framework does not have yet.
 * <p>
 * Core Flow binds plenty on its own — {@code Element.bindText},
 * {@code HasValue.bindValue}, {@code Component.bindVisible},
 * {@code AvatarGroup.bindItems}, {@code MessageList.bindItems} — and where it
 * does, the use cases use it directly. {@code Grid} is the gap: it has no
 * signal-aware items API, so UC8 and UC9 do it here. See API-GAPS.md #5.
 */
public final class SignalBindings {

    private SignalBindings() {
    }

    /**
     * Binds a grid to a shared list, keeping the item signals themselves as
     * grid items.
     * <p>
     * The indirection is what makes editing work: a signal's identity survives
     * a value change, so the grid keeps its selection and its scroll position
     * when a peer edits a row, and the column value providers read through to
     * the current value. Handing the grid the record values instead would
     * replace every item on every keystroke.
     * <p>
     * The effect reads each entry as well as the list, so an edit inside a row
     * refreshes that row, and it mutates one backing list rather than calling
     * {@code setItems} again — {@code setItems} builds a new data provider,
     * which drops selection.
     */
    public static <T> Registration bindItems(Grid<SharedValueSignal<T>> grid,
            SharedListSignal<T> list) {
        List<SharedValueSignal<T>> backing = new ArrayList<>();
        GridListDataView<SharedValueSignal<T>> dataView = grid
                .setItems(backing);
        return Signal.effect(grid, () -> {
            List<SharedValueSignal<T>> entries = list.get();
            // Subscribes to the individual rows, not just to the list shape.
            entries.forEach(Signal::get);
            backing.clear();
            backing.addAll(entries);
            dataView.refreshAll();
        });
    }
}
