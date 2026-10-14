package com.example.backend;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

/**
 * {@link SimulatedLatency} on Spring's {@link TaskScheduler}: the answer is
 * computed on a scheduler thread once the delay has passed.
 */
@Component
public class ScheduledLatency implements SimulatedLatency {

    private final TaskScheduler scheduler;

    public ScheduledLatency(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public <T> CompletableFuture<T> after(Duration delay, Supplier<T> work) {
        CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.schedule(() -> {
            if (future.isDone()) {
                return;
            }
            try {
                future.complete(work.get());
            } catch (RuntimeException e) {
                future.completeExceptionally(e);
            }
        }, Instant.now().plus(delay));
        return future;
    }

    @Override
    public void block(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
