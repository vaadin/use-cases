package com.example.usecase34;

import org.springframework.stereotype.Service;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * Application-wide feature-flag service. Owns the writable signals internally
 * and exposes only read-only {@link Signal} references via
 * {@code asReadonly()}, so views cannot mutate flag state directly — flips have
 * to go through {@link #setNewCheckoutFlow(boolean)} /
 * {@link #setBetaUi(boolean)}.
 */
@Service
public class FeatureFlagService {

    private final SharedValueSignal<Boolean> newCheckoutFlow = new SharedValueSignal<>(
            false);
    private final SharedValueSignal<Boolean> betaUi = new SharedValueSignal<>(
            false);

    public Signal<Boolean> newCheckoutFlowSignal() {
        return newCheckoutFlow.asReadonly();
    }

    public Signal<Boolean> betaUiSignal() {
        return betaUi.asReadonly();
    }

    public void setNewCheckoutFlow(boolean enabled) {
        newCheckoutFlow.set(enabled);
    }

    public void setBetaUi(boolean enabled) {
        betaUi.set(enabled);
    }
}
