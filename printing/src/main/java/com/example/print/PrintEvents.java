package com.example.print;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.MissingAPI;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;

/**
 * Server-side notification of the browser's print lifecycle.
 * <p>
 * The browser fires {@code beforeprint} and {@code afterprint} on
 * {@code window}, and Flow's {@code Element} API can only listen on elements,
 * so there is no way to hear them without writing JavaScript. This invisible
 * component does that once and turns the two events into ordinary server-side
 * listeners: add it to a view and an application can log that a document was
 * printed, stamp it with "printed by", or swap in a print-friendly rendering
 * while the page is being paginated.
 * <p>
 * The listeners are registered against an {@code AbortController} kept on
 * {@code window} under this component's own key, and
 * {@link MissingAPI#abortPrintListeners} cancels it on detach, so that
 * navigating away really removes them — a plain
 * {@code window.addEventListener} in {@code onAttach} leaks one listener per
 * visit for the lifetime of the single-page application.
 * <p>
 * Note what the events can and cannot tell you: {@code afterprint} fires when
 * the print dialog closes, whether the user printed, saved a PDF or
 * cancelled. The browser exposes no outcome, so neither does this class.
 */
public class PrintEvents extends Div {

    private final String key = "print-events-" + UUID.randomUUID();

    private final List<Runnable> beforePrintListeners = new ArrayList<>();
    private final List<Runnable> afterPrintListeners = new ArrayList<>();

    public PrintEvents() {
        // Not rendered on screen, and never on paper either.
        addClassName("no-print");
        getStyle().set("display", "none");
    }

    /**
     * Adds a listener notified when the browser is about to paginate the
     * document.
     *
     * @param listener
     *            the listener to add
     * @return a registration for removing the listener
     */
    public Registration addBeforePrintListener(Runnable listener) {
        beforePrintListeners.add(listener);
        return () -> beforePrintListeners.remove(listener);
    }

    /**
     * Adds a listener notified when the print dialog has closed, whatever the
     * user chose to do with it.
     *
     * @param listener
     *            the listener to add
     * @return a registration for removing the listener
     */
    public Registration addAfterPrintListener(Runnable listener) {
        afterPrintListeners.add(listener);
        return () -> afterPrintListeners.remove(listener);
    }

    /**
     * Invoked from the browser; public only because {@code $server} needs it.
     */
    @ClientCallable
    public void beforePrint() {
        List.copyOf(beforePrintListeners).forEach(Runnable::run);
    }

    /**
     * Invoked from the browser; public only because {@code $server} needs it.
     */
    @ClientCallable
    public void afterPrint() {
        List.copyOf(afterPrintListeners).forEach(Runnable::run);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getElement().executeJs(MissingAPI.withPrintListenerRegistry("""
                window.addEventListener('beforeprint',
                        () => self.$server.beforePrint(), { signal });
                window.addEventListener('afterprint',
                        () => self.$server.afterPrint(), { signal });
                """), key);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        MissingAPI.abortPrintListeners(detachEvent.getUI(), key);
        super.onDetach(detachEvent);
    }
}
