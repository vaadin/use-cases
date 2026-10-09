package com.example.backend;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.vaadin.flow.shared.Registration;

/**
 * A fast stream of price ticks, shared by every session: by default about a
 * hundred ticks per second spread over a dozen symbols. Ticks are produced only
 * while someone is subscribed, and are delivered on a scheduler thread.
 * <p>
 * Set {@code app.feed.ticks-per-second=0} to switch the generator off and drive
 * the feed with {@link #publish(Tick)} alone.
 */
@Component
public class MarketFeed {

    public record Tick(String symbol, double price) {
    }

    public static final List<String> SYMBOLS = List.of("AURA", "BRGT", "CBLT",
            "DLTA", "EVGN", "FXGL", "GRNT", "HRBR", "IRNL", "JNPR", "KSTR",
            "LUMN");

    private final TaskScheduler scheduler;
    private final int ticksPerSecond;
    private final List<Consumer<Tick>> subscribers = new CopyOnWriteArrayList<>();
    private final double[] prices = new double[SYMBOLS.size()];
    private @Nullable ScheduledFuture<?> generator;
    private long sequence;

    public MarketFeed(TaskScheduler scheduler,
            @Value("${app.feed.ticks-per-second:100}") int ticksPerSecond) {
        this.scheduler = scheduler;
        this.ticksPerSecond = ticksPerSecond;
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 20 + 10 * i;
        }
    }

    /**
     * Delivers every following tick to {@code subscriber}, on a background
     * thread, until the registration is removed.
     */
    public synchronized Registration subscribe(Consumer<Tick> subscriber) {
        subscribers.add(subscriber);
        if (generator == null && ticksPerSecond > 0) {
            generator = scheduler.scheduleAtFixedRate(this::generate,
                    Duration.ofNanos(1_000_000_000L / ticksPerSecond));
        }
        return () -> unsubscribe(subscriber);
    }

    private synchronized void unsubscribe(Consumer<Tick> subscriber) {
        subscribers.remove(subscriber);
        if (subscribers.isEmpty() && generator != null) {
            generator.cancel(false);
            generator = null;
        }
    }

    /** Sends one tick to every subscriber. */
    public void publish(Tick tick) {
        subscribers.forEach(subscriber -> subscriber.accept(tick));
    }

    private void generate() {
        int index;
        double price;
        synchronized (this) {
            index = (int) (sequence++ % prices.length);
            // A deterministic wobble keeps the numbers moving without a
            // random source shared across threads.
            prices[index] = Math.max(1,
                    prices[index] + Math.sin(sequence * 0.37) * 0.25);
            price = prices[index];
        }
        publish(new Tick(SYMBOLS.get(index), price));
    }
}
