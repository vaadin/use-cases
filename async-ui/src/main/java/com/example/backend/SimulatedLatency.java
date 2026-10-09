package com.example.backend;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The slow backend every use case in this module talks to. A real application
 * would call a database, a REST service or a report engine here; the demos only
 * need something that takes a while and answers on another thread.
 * <p>
 * Going through one interface keeps the views honest — they cannot tell the
 * simulation from a real remote call — and lets the browserless tests replace
 * the clock with one they complete by hand, so every "slow" answer arrives
 * exactly when, and in exactly the order, the test decides.
 */
public interface SimulatedLatency {

    /**
     * Runs {@code work} on a background thread once {@code delay} has passed.
     * The returned future completes with its result, or exceptionally with
     * whatever it threw. Cancelling the future does not stop work that has
     * already started — just like cancelling a {@link CompletableFuture}
     * wrapping a real remote call.
     */
    <T> CompletableFuture<T> after(Duration delay, Supplier<T> work);

    /**
     * Blocks the calling thread for {@code delay}: a synchronous backend call
     * made directly from a request handler or a data provider callback.
     */
    void block(Duration delay);
}
