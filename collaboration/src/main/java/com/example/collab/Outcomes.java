package com.example.collab;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.signals.operations.SignalOperation;
import com.vaadin.flow.signals.operations.SignalOperation.Error;
import com.vaadin.flow.signals.operations.SignalOperation.ResultOrError;

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
 */
public final class Outcomes {

    private Outcomes() {
    }

    /**
     * Hands the operation's outcome, formatted, to {@code sink} as soon as the
     * shared state has decided — on the session's thread.
     */
    public static void report(String action, SignalOperation<?> operation,
            SerializableConsumer<String> sink) {
        UI ui = UI.getCurrent();
        operation.result().thenAccept(outcome -> {
            Runnable deliver = () -> sink.accept(describe(action, outcome));
            if (ui == null) {
                deliver.run();
            } else {
                ui.access(deliver::run);
            }
        });
    }

    /** Appends the outcome to a log, newest line first. */
    public static void report(Div log, String action,
            SignalOperation<?> operation) {
        report(action, operation,
                line -> log.addComponentAsFirst(new Div(line)));
    }

    public static String describe(String action,
            @Nullable ResultOrError<?> outcome) {
        if (outcome == null) {
            return action + ": submitted, not confirmed yet";
        }
        if (outcome.successful()) {
            return action + ": accepted";
        }
        if (outcome instanceof Error<?> error) {
            return action + ": REJECTED — " + error.reason();
        }
        return action + ": REJECTED";
    }
}
