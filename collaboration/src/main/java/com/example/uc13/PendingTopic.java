package com.example.uc13;

import com.example.collab.Peer;
import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * UC13's shared value, plus a handle that refuses some writes so that a
 * rejection can be seen without a second server.
 * <p>
 * The validator inspects the command: {@code SetCommand} carries the value it
 * wants to write, so "no more than {@value #LIMIT} characters" is a rule the
 * shared state can enforce rather than something each client has to remember.
 * That is more than Collaboration Kit can do — and less than it looks, since
 * the validator is attached to the handle, not to the data (API-GAPS.md #11).
 */
@Component
public class PendingTopic {

    public static final int LIMIT = 12;

    private final SharedValueSignal<String> label = new SharedValueSignal<>(
            "Aisle 4");

    public SharedValueSignal<String> label() {
        return label;
    }

    /**
     * The value as a given peer may write it: short values only.
     */
    public SharedValueSignal<String> labelFor(Peer peer) {
        return label.withValidator(command -> {
            if (command instanceof SignalCommand.ValueCommand value) {
                var node = value.value();
                return node == null || !node.isTextual()
                        || node.asString().length() <= LIMIT;
            }
            return true;
        });
    }
}
