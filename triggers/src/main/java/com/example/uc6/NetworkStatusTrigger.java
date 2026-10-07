package com.example.uc6;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * Custom {@link Trigger} that fires whenever the browser reports a change in
 * online/offline status (via the {@code online} and {@code offline} window
 * events), plus once on install so the initial state is observable.
 * <p>
 * Demonstrates a trigger whose event source is {@code window}, not the host
 * element. The host is still used as the lifecycle owner — uninstall happens
 * when the host is detached.
 */
public class NetworkStatusTrigger extends Trigger {

    public NetworkStatusTrigger(Component host) {
        super(host);
    }

    @Override
    protected Registration install(JsFunction action) {
        return getHost().addJsInitializer("""
                const fire = () => $0(new Event('networkstatus'));
                window.addEventListener('online', fire);
                window.addEventListener('offline', fire);
                queueMicrotask(fire);
                return () => {
                    window.removeEventListener('online', fire);
                    window.removeEventListener('offline', fire);
                };""", action);
    }
}
