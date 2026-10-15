package com.example.collab;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.signals.operations.SignalOperation;

/**
 * Reports what the shared state made of an operation.
 * <p>
 * The indirection exists because of a detail that is easy to get wrong. Inside
 * a UI callback a command has been submitted but not yet applied:
 * {@code operation.result().getNow(null)} returns {@code null} every time, so a
 * view that reads it there reports "not confirmed yet" for operations that are
 * about to be accepted and for operations that are about to be rejected alike.
 * The result arrives when the session flushes its pending signal commands,
 * which is after the callback returns — so the only correct way to read it is a
 * callback of one's own, and since that may run on another thread, through
 * {@code ui.access}. See API-GAPS.md #14.
 * <p>
 * What is handed on is only whether the operation was accepted. The reason a
 * rejection carries is the framework's ("Unexpected value" for a
 * compare-and-set that lost), which means nothing to a user; each view words
 * its own message instead.
 */
public final class Outcomes {

    private Outcomes() {
    }

    /**
     * Tells {@code handler} whether the shared state accepted the operation, as
     * soon as it has decided — on the session's thread.
     */
    public static void whenDecided(SignalOperation<?> operation,
            SerializableConsumer<Boolean> handler) {
        UI ui = UI.getCurrent();
        operation.result().thenAccept(outcome -> {
            Runnable deliver = () -> handler.accept(outcome.successful());
            if (ui == null) {
                deliver.run();
            } else {
                ui.access(deliver::run);
            }
        });
    }
}
