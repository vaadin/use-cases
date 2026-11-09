package com.example.uc22;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.HandlerInput;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * Fires on every {@code keydown} anywhere on the page while the host is
 * attached, and exposes the {@code KeyboardEvent} properties as
 * {@link Action.Input}s on {@link EventData}.
 * <p>
 * Flow has no keyboard {@link Trigger}: a
 * {@code DomEventTrigger(host, "keydown")} sees a key press only while the
 * focus is inside the host, and has no inputs for the key or the modifiers.
 * This class is a local stand-in until Flow has one (API-GAPS.md #10). Once it
 * does, delete this class and use Flow's.
 */
public class KeyboardEventTrigger extends Trigger {

    public KeyboardEventTrigger(Component host) {
        super(host);
    }

    @Override
    protected Registration install(JsFunction action) {
        // Listen on window in the capture phase: a view with no focusable
        // children never receives a bubbled keydown on the host element.
        return getHost().addJsInitializer(
                """
                        const listener = (e) => $0(e);
                        window.addEventListener('keydown', listener, true);
                        return () => window.removeEventListener('keydown', listener, true);""",
                action);
    }

    /**
     * {@link Action.Input}s for the properties of the {@code KeyboardEvent}.
     */
    public abstract static class EventData {
        protected EventData() {
        }

        public static final Action.Input<String> key = eventProperty("key");
        public static final Action.Input<String> code = eventProperty("code");
        public static final Action.Input<Boolean> shiftKey = eventProperty(
                "shiftKey");
        public static final Action.Input<Boolean> ctrlKey = eventProperty(
                "ctrlKey");
        public static final Action.Input<Boolean> altKey = eventProperty(
                "altKey");
        public static final Action.Input<Boolean> metaKey = eventProperty(
                "metaKey");

        private static <T> Action.Input<T> eventProperty(String property) {
            return new HandlerInput<>(property, KeyboardEventTrigger.class);
        }
    }
}
