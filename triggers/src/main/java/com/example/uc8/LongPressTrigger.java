package com.example.uc8;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * Custom {@link Trigger} that fires when the host has been pressed
 * (pointerdown) for at least {@code holdMs} milliseconds without being
 * released. Cancels on pointerup, pointercancel, or pointerleave. Suppresses
 * the synthetic click that follows so the wired actions don't also receive a
 * click event.
 * <p>
 * Demonstrates a Trigger that tracks a time-windowed gesture the browser
 * doesn't fire as a single event — composing pointer events with
 * {@code setTimeout}.
 */
public class LongPressTrigger extends Trigger {

    private final int holdMs;

    public LongPressTrigger(Component host, int holdMs) {
        super(host);
        if (holdMs <= 0) {
            throw new IllegalArgumentException("holdMs must be > 0");
        }
        this.holdMs = holdMs;
    }

    @Override
    protected Registration install(JsFunction action) {
        return getHost().addJsInitializer("""
                const ms = $1;
                let timer = null;
                let triggered = false;
                const onDown = (e) => {
                    triggered = false;
                    if (timer) clearTimeout(timer);
                    timer = setTimeout(() => {
                        triggered = true;
                        $0(e);
                    }, ms);
                };
                const cancel = () => {
                    if (timer) { clearTimeout(timer); timer = null; }
                };
                const onClick = (e) => {
                    if (triggered) {
                        e.preventDefault();
                        e.stopPropagation();
                        triggered = false;
                    }
                };
                this.addEventListener('pointerdown', onDown);
                this.addEventListener('pointerup', cancel);
                this.addEventListener('pointercancel', cancel);
                this.addEventListener('pointerleave', cancel);
                this.addEventListener('click', onClick, {capture: true});
                return () => {
                    cancel();
                    this.removeEventListener('pointerdown', onDown);
                    this.removeEventListener('pointerup', cancel);
                    this.removeEventListener('pointercancel', cancel);
                    this.removeEventListener('pointerleave', cancel);
                    this.removeEventListener('click', onClick, {capture: true});
                };""", action, holdMs);
    }
}
