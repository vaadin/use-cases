package com.example.uc23;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;

/**
 * Fires when an ordered sequence of keys is pressed anywhere on the page while
 * the host is attached, e.g. the Konami code. The keys typed so far are kept in
 * the browser; only a completed sequence runs the actions, so partial progress
 * never reaches the server.
 * <p>
 * Each key of the sequence matches {@code event.key} or {@code event.code}, so
 * both {@link Key#ARROW_UP} and {@link Key#KEY_B} work. The trigger fires when
 * the last keys pressed are the sequence, whatever came before them: extra
 * presses before a correct run (↑ ↑ ↑ ↓ ↓ …) do not spoil it. There is no
 * timeout.
 * <p>
 * Flow has no trigger for a key sequence, so this class is a local stand-in
 * until it has one (API-GAPS.md #11). Once it does, delete this class and use
 * Flow's.
 */
public class SequenceTrigger extends Trigger {

    private final List<List<String>> sequenceKeys;

    public SequenceTrigger(Component host, Key... sequence) {
        super(host);
        Objects.requireNonNull(sequence, "sequence");
        if (sequence.length == 0) {
            throw new IllegalArgumentException(
                    "Sequence must contain at least one key");
        }
        List<List<String>> snapshot = new ArrayList<>(sequence.length);
        for (Key key : sequence) {
            Objects.requireNonNull(key, "key");
            snapshot.add(List.copyOf(key.getKeys()));
        }
        this.sequenceKeys = List.copyOf(snapshot);
    }

    @Override
    protected Registration install(JsFunction action) {
        // Keeps the last seq.length key presses and fires when they match the
        // sequence slot by slot. Listens on window in the capture phase: a
        // view with no focusable children never receives a bubbled keydown.
        return getHost().addJsInitializer(
                """
                        const seq = $1;
                        let recent = [];
                        const listener = (e) => {
                            recent.push([e.key, e.code]);
                            if (recent.length > seq.length) recent.shift();
                            if (recent.length === seq.length && seq.every((slot, i) =>
                                    slot.includes(recent[i][0]) || slot.includes(recent[i][1]))) {
                                recent = [];
                                $0(e);
                            }
                        };
                        window.addEventListener('keydown', listener, true);
                        return () => window.removeEventListener('keydown', listener, true);""",
                action, JacksonUtils.getMapper().valueToTree(sequenceKeys));
    }
}
