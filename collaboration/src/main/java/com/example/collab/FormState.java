package com.example.collab;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.dom.SignalBinding;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.function.CommandValidator;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedMapSignal;

/**
 * A form several users edit at once — the state behind UC5, UC6 and UC7, and
 * this module's answer to {@code CollaborationBinder} and {@code FormManager}.
 * <p>
 * Two structures, because collaborative editing is two problems:
 * <ul>
 * <li>a {@link SharedMapSignal} of field name to {@link FieldValue}, which is
 * the data, keyed per field so that two users editing different fields never
 * touch the same signal;</li>
 * <li>a {@link SharedListSignal} of peers <em>per field</em>, which is who is
 * in it right now, and what the field highlighter renders.</li>
 * </ul>
 * <p>
 * {@link #bindField} wires one field to both, and is as close as this module
 * gets to {@code binder.forField(field).bind("name")}. It is not a
 * {@code Binder}: signal binding and {@code Binder} both want to own a field's
 * value, so a form is written with one or the other. See API-GAPS.md #7.
 */
public class FormState {

    private final SharedMapSignal<FieldValue> values;
    private final Map<String, SharedListSignal<Peer>> editors;

    public FormState() {
        this(new SharedMapSignal<>(FieldValue.class),
                new ConcurrentHashMap<>());
    }

    private FormState(SharedMapSignal<FieldValue> values,
            Map<String, SharedListSignal<Peer>> editors) {
        this.values = values;
        this.editors = editors;
    }

    /**
     * The same form, with every write checked by {@code validator} first.
     * <p>
     * The returned state shares this one's node in the signal tree — and its
     * editor lists — so the two views the same data; only the writes go through
     * the validator. That is how UC7 enforces write access in the shared state
     * rather than in each client's UI.
     */
    public FormState withValidator(CommandValidator validator) {
        return new FormState(values.withValidator(validator), editors);
    }

    /** The form as something that cannot be written to at all. */
    public FormState asReadonly() {
        return new FormState(values.asReadonly(), editors);
    }

    public SharedMapSignal<FieldValue> values() {
        return values;
    }

    /** The current value of one field, empty string when never written. */
    public Signal<String> value(String field) {
        return entry(field).map(value -> value.value());
    }

    /** The whole entry, including who last wrote it. */
    public Signal<FieldValue> entry(String field) {
        return Signal.computed(() -> {
            var signal = values.get().get(field);
            FieldValue current = signal == null ? null : signal.get();
            return current == null ? FieldValue.EMPTY : current;
        });
    }

    public void write(String field, String value, Peer author) {
        values.put(field, new FieldValue(value == null ? "" : value,
                author.id(), author.name()));
    }

    /** Who is currently in a field. Created on first use, like a topic. */
    public SharedListSignal<Peer> editors(String field) {
        return editors.computeIfAbsent(field,
                key -> new SharedListSignal<>(Peer.class));
    }

    public void enter(String field, Peer peer) {
        SharedListSignal<Peer> list = editors(field);
        if (!isEditing(field, peer)) {
            list.insertLast(peer);
        }
    }

    public void leave(String field, Peer peer) {
        SharedListSignal<Peer> list = editors(field);
        // No remove-by-value on a shared list: the entry has to be found
        // first, and the signal identity is what remove() takes.
        list.peek().stream().filter(entry -> {
            Peer value = entry.peek();
            return value != null && value.id() == peer.id();
        }).findFirst().ifPresent(list::remove);
    }

    private boolean isEditing(String field, Peer peer) {
        return editors(field).peekValues()
                .anyMatch(value -> value != null && value.id() == peer.id());
    }

    /**
     * Binds one field to the shared value and to the highlighter, and reports
     * this peer as editing it while it has focus.
     * <p>
     * Focus is observed as a DOM event rather than through
     * {@code Focusable#addFocusListener}, because that method is typed on the
     * concrete component and this helper takes any field.
     * <p>
     * Nothing is returned to unbind with, because there is nothing to return:
     * {@code bindValue} hands back a {@code SignalBinding}, which is not a
     * {@code Registration} and offers no way to undo the binding (API-GAPS.md
     * #10). Everything here therefore lives and dies with the field — which is
     * what the use cases want anyway.
     *
     * @return the value binding, whose {@code onChange} is where UC6 listens
     *         for old-and-new value pairs
     */
    public <F extends Component & HasValue<?, String>> SignalBinding<String> bindField(
            F field, String name, Peer peer) {
        SignalFieldHighlighter.bind(field, editors(name), peer);

        field.getElement().addEventListener("focus",
                event -> enter(name, peer));
        field.getElement().addEventListener("blur", event -> leave(name, peer));
        // A peer that is gone is not editing anything, whatever the last DOM
        // event said.
        field.addDetachListener(event -> leave(name, peer));

        return field.bindValue(value(name),
                written -> write(name, written, peer));
    }
}
