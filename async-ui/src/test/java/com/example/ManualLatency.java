package com.example;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.example.backend.SimulatedLatency;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * {@link SimulatedLatency} for browserless tests: nothing happens until the
 * test says so. Slow calls queue up in the order they were made, and the test
 * completes them one by one, in any order, on its own thread — so "the
 * dashboard's third widget answers first" or "the older search answers last" is
 * a deterministic test instead of a race.
 * <p>
 * Completing a call runs its callbacks on the test thread, which queues their
 * {@code UI.access} commands; the test then runs those with
 * {@code roundTrip()}. Blocking calls return at once.
 * <p>
 * Being a {@link Primary} {@link Component} in the test sources, it replaces
 * the scheduler-based implementation in every test's application context.
 */
@Component
@Primary
public class ManualLatency implements SimulatedLatency {

    /** One slow call that has not answered yet. */
    public record Call<T>(Duration delay, Supplier<T> work,
            CompletableFuture<T> future) {

        void complete() {
            if (future.isDone()) {
                return;
            }
            try {
                future.complete(work.get());
            } catch (RuntimeException e) {
                future.completeExceptionally(e);
            }
        }
    }

    private final List<Call<?>> pending = new ArrayList<>();
    private final List<Duration> blocked = new ArrayList<>();

    @Override
    public synchronized <T> CompletableFuture<T> after(Duration delay,
            Supplier<T> work) {
        CompletableFuture<T> future = new CompletableFuture<>();
        pending.add(new Call<>(delay, work, future));
        return future;
    }

    @Override
    public synchronized void block(Duration delay) {
        blocked.add(delay);
    }

    /** The calls still waiting for an answer, oldest first. */
    public synchronized List<Call<?>> pending() {
        return List.copyOf(pending);
    }

    /** Answers the {@code index}th pending call, counting from the oldest. */
    public void complete(int index) {
        Call<?> call;
        synchronized (this) {
            call = pending.remove(index);
        }
        call.complete();
    }

    /** Makes the oldest pending call fail with {@code error}. */
    public void failNext(RuntimeException error) {
        Call<?> call;
        synchronized (this) {
            call = pending.removeFirst();
        }
        call.future().completeExceptionally(error);
    }

    /** Answers the oldest pending call. */
    public void completeNext() {
        complete(0);
    }

    /**
     * Answers every call pending right now, oldest first. Calls those answers
     * start in turn stay pending.
     */
    public void completePending() {
        List<Call<?>> calls;
        synchronized (this) {
            calls = List.copyOf(pending);
            pending.clear();
        }
        calls.forEach(Call::complete);
    }

    /** The blocking calls made so far. */
    public synchronized List<Duration> blocked() {
        return List.copyOf(blocked);
    }

    /** Forgets every call, so one test cannot see another's. */
    public synchronized void reset() {
        pending.clear();
        blocked.clear();
    }
}
