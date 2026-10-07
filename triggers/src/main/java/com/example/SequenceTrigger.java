package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;

/**
 * Fires when a specific ordered sequence of keys is pressed on the host — e.g.
 * the Konami code or a "hello" Easter egg. State is tracked client-side; only a
 * completed sequence produces a server-side fire, so partial progress never
 * crosses the network.
 * <p>
 * Each key in the sequence matches against {@code event.key} or
 * {@code event.code}, so both event.key-named keys (e.g. {@link Key#ENTER}) and
 * event.code-named keys (e.g. {@link Key#KEY_S}) work. A wrong key resets the
 * position to 0; if that wrong key happens to match position 0, the position
 * advances to 1 (so {@code "abab"} on the sequence {@code "abab"} completes
 * correctly). No timeout — a partial sequence persists across arbitrary gaps.
 * <p>
 * A local stand-in for the {@code SequenceTrigger} on
 * {@code vaadin/flow:feature/triggers-actions}; rewritten on top of the
 * mainline {@link Trigger} contract. Delete when the upstream class lands.
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
        String sequenceJson = sequenceKeys.stream()
                .map(slot -> slot.stream().map(
                        s -> JacksonUtils.getMapper().valueToTree(s).toString())
                        .collect(Collectors.joining(",", "[", "]")))
                .collect(Collectors.joining(",", "[", "]"));

        StringBuilder js = new StringBuilder();
        js.append("const seq=").append(sequenceJson).append(";");
        js.append("let i=0;");
        js.append("const listener=(e)=>{");
        js.append("const o=s=>s.includes(e.key)||s.includes(e.code);");
        // Mismatch — restart at 1 if the key still matches slot 0, else 0.
        js.append("if(!o(seq[i])){i=o(seq[0])?1:0;return;}");
        // Match — advance; if not yet at end, return without firing.
        js.append("if(++i!==seq.length)return;i=0;");
        js.append("$0(e);");
        js.append("};");
        // Listen on window in the capture phase: a view with no focusable
        // children never receives bubbled keydown on the host element.
        js.append("window.addEventListener('keydown',listener,true);");
        js.append(
                "return ()=>window.removeEventListener('keydown',listener,true);");
        return getHost().addJsInitializer(js.toString(), action);
    }
}
