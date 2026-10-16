package com.example.uc1;

import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * The maintenance notice shown to every user of the application.
 * <p>
 * A plain Spring {@code @Component} is a singleton: one instance for the whole
 * application, shared by every user, session and browser tab. This is what
 * {@code @ApplicationScoped} means in CDI. Because many sessions read and write
 * it concurrently, the state lives in a {@link SharedValueSignal}, which is
 * thread-safe and pushes changes to every UI that shows it.
 */
@Component
public class MaintenanceNotice {

    private final SharedValueSignal<String> message = new SharedValueSignal<>(
            "");

    public Signal<String> message() {
        return message.asReadonly();
    }

    public void publish(String text) {
        message.set(text.strip());
    }

    public void clear() {
        message.set("");
    }
}
