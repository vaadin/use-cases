package com.example.uc11;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Counts the open sessions and UIs on this server, to watch while a load test
 * runs (UC11).
 */
@Component
public class SessionCounter implements VaadinServiceInitListener {

    private final AtomicInteger sessions = new AtomicInteger();
    private final AtomicInteger uis = new AtomicInteger();
    private final AtomicInteger totalSessions = new AtomicInteger();

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(e -> {
            sessions.incrementAndGet();
            totalSessions.incrementAndGet();
        });
        event.getSource()
                .addSessionDestroyListener(e -> sessions.decrementAndGet());
        event.getSource().addUIInitListener(e -> {
            uis.incrementAndGet();
            e.getUI().addDetachListener(d -> uis.decrementAndGet());
        });
    }

    public int sessions() {
        return sessions.get();
    }

    public int uis() {
        return uis.get();
    }

    public int totalSessions() {
        return totalSessions.get();
    }
}
