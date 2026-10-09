package com.example.uc7;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import com.example.backend.MarketFeed;
import com.example.backend.MarketFeed.Tick;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

/**
 * UC7 — Throttle a fast live feed.
 * <p>
 * A price feed produces about a hundred ticks per second. Pushing each tick to
 * the browser as it arrives means a hundred rounds of change collection,
 * serialisation and websocket messages per second per user — far more than a
 * person can read. Batching keeps only the latest price per symbol and pushes
 * them together a few times per second: the screen looks the same and the
 * server and the network do a fraction of the work. The counters show the
 * difference.
 * <p>
 * Flow pushes once per {@link UI#access} and has no built-in way to merge them,
 * so the batching is done by hand (see API-GAPS.md).
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Throttle a live feed")
@UseCaseDescription("Merging a fast stream of updates into a few pushes per second")
@Menu(order = 7, title = "UC7 — Throttle a live feed")
@StyleSheet("uc7.css")
public class ThrottledFeedView extends VerticalLayout {

    static final Duration FLUSH_INTERVAL = Duration.ofMillis(250);

    enum Mode {
        EVERY_TICK("Push every tick"), BATCHED("Batch every 250 ms");

        private final String label;

        Mode(String label) {
            this.label = label;
        }
    }

    private final MarketFeed feed;
    private final TaskScheduler scheduler;

    private final RadioButtonGroup<Mode> mode = new RadioButtonGroup<>(
            "Delivery");
    private final Map<String, Span> priceLabels = new LinkedHashMap<>();
    private final Map<String, Tick> latest = new ConcurrentHashMap<>();
    private final AtomicLong ticksReceived = new AtomicLong();
    private final Span stats = new Span();
    private long uiUpdates;
    // Read by the feed thread, which must not ask the RadioButtonGroup.
    private volatile boolean everyTick;

    private @Nullable Registration subscription;
    private @Nullable ScheduledFuture<?> flusher;

    public ThrottledFeedView(MarketFeed feed, TaskScheduler scheduler) {
        this.feed = feed;
        this.scheduler = scheduler;
        addClassName("uc7-view");

        add(new H1("UC7 — Throttle a live feed"));
        add(new Paragraph("The board below follows a feed of about 100 "
                + "price ticks per second. Switch between pushing every "
                + "tick and batching: the prices look just as live, but the "
                + "number of UI updates sent to the browser drops from "
                + "about 100 to 4 per second."));

        mode.setItems(Mode.values());
        mode.setItemLabelGenerator(m -> m.label);
        mode.setValue(Mode.BATCHED);
        mode.addValueChangeListener(event -> {
            everyTick = event.getValue() == Mode.EVERY_TICK;
            resetCounters();
        });

        Div board = new Div();
        board.addClassName("price-board");
        for (String symbol : MarketFeed.SYMBOLS) {
            Span name = new Span(symbol);
            name.addClassName("symbol");
            Span price = new Span("–");
            price.addClassName("price");
            priceLabels.put(symbol, price);
            board.add(new Div(name, price));
        }

        stats.addClassName("feed-stats");
        add(mode, stats, board);
        renderStats();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        subscription = feed.subscribe(tick -> onTick(ui, tick));
        flusher = scheduler.scheduleAtFixedRate(
                ui.accessLater(this::flush, null), FLUSH_INTERVAL);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (subscription != null) {
            subscription.remove();
            subscription = null;
        }
        if (flusher != null) {
            flusher.cancel(false);
            flusher = null;
        }
        super.onDetach(detachEvent);
    }

    /** Runs on the feed's thread: must not touch components directly. */
    private void onTick(UI ui, Tick tick) {
        ticksReceived.incrementAndGet();
        if (everyTick) {
            ui.accessLater(() -> {
                show(tick);
                uiUpdates++;
                renderStats();
            }, null).run();
        } else {
            latest.put(tick.symbol(), tick);
        }
    }

    /** Applies the latest tick of each symbol: one UI update. */
    private void flush() {
        if (latest.isEmpty()) {
            return;
        }
        for (String symbol : MarketFeed.SYMBOLS) {
            Tick tick = latest.remove(symbol);
            if (tick != null) {
                show(tick);
            }
        }
        uiUpdates++;
        renderStats();
    }

    private void show(Tick tick) {
        Objects.requireNonNull(priceLabels.get(tick.symbol()))
                .setText("%.2f".formatted(tick.price()));
    }

    private void resetCounters() {
        latest.clear();
        ticksReceived.set(0);
        uiUpdates = 0;
        renderStats();
    }

    private void renderStats() {
        stats.setText("%,d ticks received, %,d UI updates pushed"
                .formatted(ticksReceived.get(), uiUpdates));
    }

    // Package-private test seams.
    long uiUpdates() {
        return uiUpdates;
    }

    void flushNow() {
        flush();
    }

    String priceOf(String symbol) {
        return Objects.requireNonNull(priceLabels.get(symbol)).getText();
    }
}
