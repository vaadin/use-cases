package com.example.listeners;

import com.example.security.CurrentUserSignal;
import com.example.signals.SessionIdHelper;
import com.example.signals.UserSessionRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;

/**
 * Global interceptor that tracks all user interactions by intercepting UIDL
 * requests. UIDL requests represent all user interactions including: - Button
 * clicks - Text field input - Form submissions - Component interactions -
 * Navigation events
 *
 * This interceptor automatically updates the last interaction time in the
 * UserSessionRegistry for active sessions.
 *
 * Uses VaadinRequestInterceptor (since Flow 24.2) which is specifically
 * designed for aspect-like request observation without interfering with request
 * processing.
 */
@Component
public class UserInteractionTracker implements VaadinServiceInitListener {

    private final UserSessionRegistry userSessionRegistry;
    private final ObjectProvider<CurrentUserSignal> currentUserSignal;

    public UserInteractionTracker(UserSessionRegistry userSessionRegistry,
            ObjectProvider<CurrentUserSignal> currentUserSignal) {
        this.userSessionRegistry = userSessionRegistry;
        this.currentUserSignal = currentUserSignal;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addVaadinRequestInterceptor(new VaadinRequestInterceptor() {
            @Override
            public void requestStart(VaadinRequest request,
                    VaadinResponse response) {
                // Only track UIDL requests (user interactions)
                if (HandlerHelper.isRequestType(request,
                        HandlerHelper.RequestType.UIDL)) {
                    VaadinSession session = VaadinSession.getCurrent();
                    if (session != null) {
                        trackInteraction(session);
                    }
                }
            }

            @Override
            public void handleException(VaadinRequest request,
                    VaadinResponse response, VaadinSession vaadinSession,
                    Exception t) {
                // No action needed for exceptions
            }

            @Override
            public void requestEnd(VaadinRequest request,
                    VaadinResponse response, VaadinSession session) {
                // No action needed at request end
            }
        });
    }

    private void trackInteraction(VaadinSession session) {
        // Access session synchronously to get user information
        session.access(() -> {
            try {
                // Session-scoped bean, so resolve it inside the session lock
                String username = currentUserSignal.getObject().getUserSignal()
                        .peek().getUsername();
                String sessionId = SessionIdHelper.getCurrentSessionId();

                if (username != null && sessionId != null) {
                    userSessionRegistry.updateLastInteraction(username,
                            sessionId);
                }
            } catch (Exception e) {
                // Silently ignore errors - don't break request handling
                // This can happen if UI is not available or session is closing
            }
        });
    }
}
