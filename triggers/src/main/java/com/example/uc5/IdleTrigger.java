package com.example.uc5;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * Custom {@link Trigger} that fires when the user has been inactive for at
 * least {@code inactivityMs} milliseconds, and again when activity resumes
 * after an idle period. The trigger keeps a {@code setTimeout} timer that any
 * keyboard/pointer/scroll event on {@code window} resets.
 * <p>
 * The handler receives a synthetic {@code CustomEvent} with
 * {@code event.detail.idle === true|false}; the {@link EventData#idle} input
 * exposes that as a server-decodable {@code Boolean}.
 * <p>
 * Demonstrates a Trigger whose state lives entirely in JS — no DOM event maps
 * to "idle"; the trigger composes setTimeout with regular activity listeners.
 */
public class IdleTrigger extends Trigger {

    private final int inactivityMs;

    public IdleTrigger(Component host, int inactivityMs) {
        super(host);
        if (inactivityMs < 0) {
            throw new IllegalArgumentException("inactivityMs must be >= 0");
        }
        this.inactivityMs = inactivityMs;
    }

    @Override
    protected Registration install(JsFunction action) {
        // The handler is fired twice per idle cycle: once when the timer
        // elapses (idle=true) and once on the first activity after that
        // (idle=false). Initial state is "active"; we don't fire on install.
        return getHost().addJsInitializer(
                """
                        const ms = $1;
                        const events = ['mousemove','mousedown','keydown','wheel','touchstart'];
                        let idle = false;
                        let timer = null;
                        const fire = (state) => $0(new CustomEvent('idle', {detail: {idle: state}}));
                        const goIdle = () => { idle = true; fire(true); };
                        const onActivity = () => {
                            if (idle) { idle = false; fire(false); }
                            if (timer) clearTimeout(timer);
                            timer = setTimeout(goIdle, ms);
                        };
                        for (const e of events) window.addEventListener(e, onActivity, {passive: true});
                        onActivity();
                        return () => {
                            if (timer) clearTimeout(timer);
                            for (const e of events) window.removeEventListener(e, onActivity);
                        };""",
                action, inactivityMs);
    }

    /** Inputs exposed by this trigger's handler scope. */
    public abstract static class EventData {
        protected EventData() {
        }

        /**
         * {@code event.detail.idle} — {@code true} when the trigger fired
         * because the user just became idle, {@code false} when activity
         * resumed.
         */
        public static final Action.Input<Boolean> idle = new Action.Input<>() {
            @Override
            public JsFunction toJs(Trigger trigger) {
                if (!(trigger instanceof IdleTrigger)) {
                    throw new IllegalArgumentException(
                            "Input is scoped to IdleTrigger and cannot be used in a "
                                    + trigger.getClass().getSimpleName()
                                    + " handler");
                }
                return JsFunction.of("return event.detail.idle")
                        .withArguments("event");
            }
        };
    }
}
