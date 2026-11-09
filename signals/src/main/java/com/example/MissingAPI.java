package com.example;

import java.io.Serializable;
import java.util.UUID;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Temporary helper class providing static methods for Signal-based component
 * bindings until the official API is implemented in Vaadin.
 */
public class MissingAPI {

    /**
     * Synchronizes the selected index between a Tabs component and a writable
     * signal.
     * <p>
     * This method establishes bidirectional binding between a {@code Tabs}
     * instance and a writable integer signal. Changes to the selected tab in
     * the UI will update the signal's value, and changes to the signal's value
     * will update the selected tab in the UI.
     *
     * @param tabs
     *            The Tabs component whose selected index should be synchronized
     *            with the signal. Must not be {@code null}.
     * @param numberSignal
     *            A writable integer signal that will receive the current
     *            selected index of the Tabs and can also update it. Must not be
     *            {@code null}.
     * @param writeCallback
     *            A callback invoked to write the new selected index back to the
     *            signal when the user changes the tab in the UI. Must not be
     *            {@code null}.
     */
    public static void tabsSyncSelectedIndex(TabSheet tabs,
            Signal<Integer> numberSignal,
            SerializableConsumer<Integer> writeCallback) {
        Signal.effect(tabs, () -> {
            Integer index = numberSignal.get();
            if (index != null) {
                tabs.setSelectedIndex(index);
            }
        });
        tabs.addSelectedChangeListener(event -> {
            writeCallback.accept(event.getSource().getSelectedIndex());
        });
    }

    /**
     * Extracts the current values from a {@link ListSignal} as a stream.
     * <p>
     * Each entry in the list signal is itself a signal; this method unwraps
     * them by calling {@link Signal#get()} on each entry.
     *
     * @param <T>
     *            the type of values in the list signal
     * @param signal
     *            the list signal to extract values from. Must not be
     *            {@code null}.
     * @return a stream of the current values
     */
    public static <T> Stream<T> getValues(ListSignal<T> signal) {
        return signal.get().stream().map(Signal::get);
    }

    /**
     * A component's size in pixels, mirroring the future {@code Component.Size}
     * API from vaadin/flow#23618.
     */
    public record ComponentSize(int width, int height) implements Serializable {
    }

    private static final String RESIZE_EVENT = "component-resize";

    /**
     * Creates a read-only signal that tracks the size of the given component's
     * element using a ResizeObserver.
     * <p>
     * This mirrors the future {@code Component.sizeSignal()} API from
     * vaadin/flow#23618. When that PR merges, replace
     * {@code MissingAPI.sizeSignal(component)} with
     * {@code component.sizeSignal()}.
     */
    public static Signal<ComponentSize> sizeSignal(Component component) {
        ValueSignal<ComponentSize> signal = new ValueSignal<>(
                new ComponentSize(0, 0));

        component.addAttachListener(attachEvent -> {
            String cleanupKey = UUID.randomUUID().toString();

            DomListenerRegistration reg = component.getElement()
                    .addEventListener(RESIZE_EVENT, event -> {
                        signal.set(event.getEventDetail(ComponentSize.class));
                    }).addEventData("event.detail");

            component.getElement().executeJs(ResizeObserverJs.class)
                    .observe(cleanupKey, RESIZE_EVENT);

            component.addDetachListener(detachEvent -> {
                detachEvent.unregisterListener();
                reg.remove();
                detachEvent.getUI().getPage().executeJs(ResizeObserverJs.class)
                        .disconnect(cleanupKey);
            });
        });

        return signal.asReadonly();
    }

    @JsDefinition
    private interface ResizeObserverJs extends Serializable {

        @JsExpression("""
                const resizeObserver = new ResizeObserver(entries => {
                    for (let entry of entries) {
                        const width = Math.floor(entry.contentRect.width);
                        const height = Math.floor(entry.contentRect.height);
                        const event = new CustomEvent($1, {
                            detail: { width: width, height: height }
                        });
                        this.dispatchEvent(event);
                    }
                });
                resizeObserver.observe(this);
                window[$0] = resizeObserver;
                const rect = this.getBoundingClientRect();
                const event = new CustomEvent($1, {
                    detail: { width: Math.floor(rect.width), height: Math.floor(rect.height) }
                });
                this.dispatchEvent(event);
                """)
        void observe(String key, String eventName);

        @JsExpression("""
                if (window[$0]) {
                    window[$0].disconnect();
                    delete window[$0];
                }
                """)
        void disconnect(String key);
    }
}
