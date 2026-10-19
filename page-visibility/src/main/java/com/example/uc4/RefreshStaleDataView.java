package com.example.uc4;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.PageVisibility;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — Refresh stale data.
 * <p>
 * The "USD/EUR" rate card refreshes automatically when the tab becomes visible
 * again after at least 5 seconds of being hidden, whether or not its window has
 * the focus yet. Quick alt-tabs do not trigger a fetch — useful to avoid
 * flicker when the user is just glancing away. A manual refresh button is
 * always available.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Refresh stale data")
@UseCaseDescription("Refreshing stale data when the user returns to the tab")
@Menu(order = 4, title = "UC4 — Refresh stale data")
@StyleSheet("uc4.css")
public class RefreshStaleDataView extends VerticalLayout {

    static final Duration REFRESH_THRESHOLD = Duration.ofSeconds(5);

    private static final Duration HIGHLIGHT_DURATION = Duration.ofMillis(600);

    private final TaskScheduler taskScheduler;

    private final ValueSignal<Double> rate = new ValueSignal<>(1.0823);
    private final ValueSignal<Instant> updatedAt = new ValueSignal<>(
            Instant.now());
    private final ValueSignal<Instant> now = new ValueSignal<>(Instant.now());
    private final ValueSignal<Boolean> highlighted = new ValueSignal<>(false);

    private @Nullable Instant hiddenAt;
    private @Nullable ScheduledFuture<?> tickTask;
    private @Nullable ScheduledFuture<?> highlightTask;

    public RefreshStaleDataView(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;

        addClassName("uc4-view");
        add(new H1("UC4 — Refresh stale data"));
        add(new Paragraph("The card below shows a fake USD/EUR exchange "
                + "rate. Switch away for more than 5 seconds and back — "
                + "the rate auto-refreshes and the card highlights. "
                + "Quick glances away (under 5 s) are ignored."));

        Span pair = new Span("USD/EUR");
        pair.addClassName("uc4-pair");
        Span valueLabel = new Span();
        valueLabel.addClassName("uc4-rate");
        valueLabel.bindText(rate.map("%.4f"::formatted));
        Span timestampLabel = new Span();
        timestampLabel.addClassName("uc4-timestamp");
        timestampLabel.bindText(Signal.computed(() -> "Last updated "
                + Duration.between(updatedAt.get(), now.get()).toSeconds()
                + " s ago"));

        Card card = new Card();
        card.addClassName("uc4-card");
        card.addThemeVariants(CardVariant.OUTLINED);
        card.setHeader(pair);
        card.add(valueLabel);
        card.addToFooter(timestampLabel);
        card.bindClassName("uc4-highlight", highlighted);

        Button manual = new Button("Refresh now", e -> refresh());
        add(card, manual);

        Signal<PageVisibility> visibility = UI.getCurrent().getPage()
                .pageVisibilitySignal();
        Signal.effect(this, () -> {
            PageVisibility state = visibility.get();
            Instant at = Instant.now();
            if (state == PageVisibility.HIDDEN) {
                if (hiddenAt == null) {
                    hiddenAt = at;
                }
                stopTimestampTick();
            } else {
                if (state != PageVisibility.UNKNOWN) {
                    if (isStale(hiddenAt, at)) {
                        refresh();
                    }
                    hiddenAt = null;
                }
                now.set(at);
                startTimestampTick();
            }
        });
        // The effect is paused on detach, but that does not cancel the tasks
        // it has already started.
        whenAttached(ui -> () -> {
            stopTimestampTick();
            if (highlightTask != null) {
                highlightTask.cancel(false);
                highlightTask = null;
            }
        });
    }

    /**
     * Whether a tab that was hidden at {@code hiddenAt} and is visible again at
     * {@code returnedAt} was away long enough for its data to be refreshed.
     */
    static boolean isStale(@Nullable Instant hiddenAt, Instant returnedAt) {
        return hiddenAt != null && Duration.between(hiddenAt, returnedAt)
                .compareTo(REFRESH_THRESHOLD) >= 0;
    }

    private void refresh() {
        rate.update(r -> Math.max(0.5, Math.min(2.0,
                r + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.01)));
        Instant at = Instant.now();
        updatedAt.set(at);
        now.set(at);
        highlight();
    }

    private void highlight() {
        highlighted.set(true);
        if (highlightTask != null) {
            highlightTask.cancel(false);
        }
        highlightTask = taskScheduler.schedule(() -> highlighted.set(false),
                Instant.now().plus(HIGHLIGHT_DURATION));
    }

    private void startTimestampTick() {
        if (tickTask != null) {
            return;
        }
        tickTask = taskScheduler.scheduleAtFixedRate(
                () -> now.set(Instant.now()),
                Instant.now().plus(Duration.ofSeconds(1)),
                Duration.ofSeconds(1));
    }

    private void stopTimestampTick() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
        }
    }

    // Package-private test seams: avoid reflection in
    // RefreshStaleDataViewTest while keeping the fields encapsulated.
    double currentRate() {
        return rate.peek();
    }

    void backdateHiddenAt(long secondsAgo) {
        this.hiddenAt = Instant.now().minusSeconds(secondsAgo);
    }
}
