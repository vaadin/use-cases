package com.example.print;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * A {@link Trigger} that fires on the browser's {@code beforeprint} or
 * {@code afterprint} event.
 * <p>
 * This is how to change what ends up on paper from Java. The browser lays out
 * the printed pages synchronously right after {@code beforeprint}, so a
 * server-side listener such as {@link PrintEvents} always answers too late. The
 * actions wired to this trigger run in the browser itself, inside the event,
 * before the layout happens — and the matching {@link #afterPrint(Component)}
 * trigger puts the screen back once the dialog closes.
 * <p>
 * The events fire on {@code window}, which outlives every view. The host
 * component only provides the lifecycle: the listener is installed when the
 * host is attached and removed when it is detached, so revisiting a view does
 * not pile up another listener.
 * <p>
 * Built on Flow's trigger API, which still lives in an {@code internal}
 * package; see {@code API-GAPS.md}.
 */
public final class PrintTrigger extends Trigger {

    private final String eventName;

    private PrintTrigger(Component host, String eventName) {
        super(host);
        this.eventName = eventName;
    }

    /**
     * A trigger that fires just before the browser lays out the printed pages.
     *
     * @param host
     *            the component whose lifecycle the listener follows
     * @return a new trigger; wire at least one action to it
     */
    public static PrintTrigger beforePrint(Component host) {
        return new PrintTrigger(host, "beforeprint");
    }

    /**
     * A trigger that fires when the print dialog has closed, whatever the user
     * chose to do with it.
     *
     * @param host
     *            the component whose lifecycle the listener follows
     * @return a new trigger; wire at least one action to it
     */
    public static PrintTrigger afterPrint(Component host) {
        return new PrintTrigger(host, "afterprint");
    }

    @Override
    protected Registration install(JsFunction action) {
        return getHost().addJsInitializer("""
                const listener = event => $0(event);
                window.addEventListener($1, listener);
                return () => window.removeEventListener($1, listener);""",
                action, eventName);
    }
}
