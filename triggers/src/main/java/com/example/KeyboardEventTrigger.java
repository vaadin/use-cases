package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;

/**
 * Fires on a DOM keyboard event ({@code keydown} by default). Exposes the
 * {@code KeyboardEvent} properties as static {@link Action.Input} fields on
 * {@link EventData}, and supports filtering by key via {@link #forKeys(Key...)}
 * so only the configured keys produce a server-side fire.
 * <p>
 * A local stand-in for the {@code KeyboardEventTrigger} on
 * {@code vaadin/flow:feature/triggers-actions}. The upstream class extends the
 * feature branch's {@code DomEventTrigger} with extension hooks that mainline
 * doesn't have; this version sits directly on {@link Trigger}. Delete this file
 * once the upstream class lands and switch the imports.
 */
public class KeyboardEventTrigger extends Trigger {

    private final String eventName;
    private final List<Key> keyFilter = new ArrayList<>();
    private boolean preventDefault;
    private boolean stopPropagation;

    public KeyboardEventTrigger(Component host) {
        this(host, "keydown");
    }

    public KeyboardEventTrigger(Component host, String eventName) {
        super(host);
        this.eventName = Objects.requireNonNull(eventName);
    }

    /**
     * Restricts the trigger to fire only when {@code event.key} or
     * {@code event.code} matches one of the given keys. Multiple calls
     * accumulate.
     */
    public KeyboardEventTrigger forKeys(Key... keys) {
        Objects.requireNonNull(keys, "keys");
        for (Key key : keys) {
            keyFilter.add(Objects.requireNonNull(key, "key"));
        }
        return this;
    }

    public KeyboardEventTrigger preventDefault() {
        this.preventDefault = true;
        return this;
    }

    public KeyboardEventTrigger stopPropagation() {
        this.stopPropagation = true;
        return this;
    }

    @Override
    protected Registration install(JsFunction action) {
        String quotedEvent = JacksonUtils.getMapper().valueToTree(eventName)
                .toString();
        StringBuilder js = new StringBuilder();
        js.append("const listener=(e)=>{");
        if (!keyFilter.isEmpty()) {
            String allowed = keyFilter.stream()
                    .flatMap(k -> k.getKeys().stream()).distinct()
                    .map(s -> JacksonUtils.getMapper().valueToTree(s)
                            .toString())
                    .collect(Collectors.joining(",", "[", "]"));
            js.append("const allowed=").append(allowed).append(";");
            js.append(
                    "if(!allowed.includes(e.key)&&!allowed.includes(e.code))return;");
        }
        if (preventDefault) {
            js.append("e.preventDefault();");
        }
        if (stopPropagation) {
            js.append("e.stopPropagation();");
        }
        js.append("$0(e);");
        js.append("};");
        // Listen on window in the capture phase: a view with no focusable
        // children never receives bubbled keydown on the host element.
        js.append("window.addEventListener(").append(quotedEvent)
                .append(",listener,true);");
        js.append("return ()=>window.removeEventListener(").append(quotedEvent)
                .append(",listener,true);");
        return getHost().addJsInitializer(js.toString(), action);
    }

    /** Static {@link Action.Input} sources for KeyboardEvent properties. */
    public abstract static class EventData {
        protected EventData() {
        }

        public static final Action.Input<String> key = eventProperty("key");
        public static final Action.Input<String> code = eventProperty("code");
        public static final Action.Input<Boolean> repeat = eventProperty(
                "repeat");
        public static final Action.Input<Boolean> isComposing = eventProperty(
                "isComposing");
        public static final Action.Input<Boolean> shiftKey = eventProperty(
                "shiftKey");
        public static final Action.Input<Boolean> ctrlKey = eventProperty(
                "ctrlKey");
        public static final Action.Input<Boolean> altKey = eventProperty(
                "altKey");
        public static final Action.Input<Boolean> metaKey = eventProperty(
                "metaKey");

        private static <T> Action.Input<T> eventProperty(String prop) {
            String quoted = JacksonUtils.getMapper().valueToTree(prop)
                    .toString();
            return new Action.Input<T>() {
                @Override
                public JsFunction toJs(Trigger trigger) {
                    if (!(trigger instanceof KeyboardEventTrigger)) {
                        throw new IllegalArgumentException(
                                "Input is scoped to KeyboardEventTrigger and cannot be used in a "
                                        + trigger.getClass().getSimpleName()
                                        + " handler");
                    }
                    return JsFunction.of("return event[" + quoted + "]")
                            .withArguments("event");
                }
            };
        }
    }
}
