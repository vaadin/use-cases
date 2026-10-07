package com.example.uc4;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC4 — Refresh stale data when the user returns.
 * <p>
 * The "USD/EUR" rate card refreshes automatically when the tab regains
 * visibility after at least 5 seconds of being hidden. Quick alt-tabs do not
 * trigger a fetch — useful to avoid flicker when the user is just glancing
 * away. A manual refresh button is always available.
 */
@Route(value = "uc4", layout = MainLayout.class)
@Menu(order = 4, title = "UC4 — Refresh stale data")
@StyleSheet("uc4.css")
public class RefreshStaleDataView extends VerticalLayout {

    private static final long REFRESH_THRESHOLD_SECONDS = 5;

    private final TaskScheduler taskScheduler;

    private double rate = 1.0823;
    private Instant updatedAt = Instant.now();
    private @Nullable Instant hiddenAt;
    private PageVisibility prevState = PageVisibility.VISIBLE;

    private final Span valueLabel = new Span();
    private final Span timestampLabel = new Span();
    private final Card card = new Card();

    private @Nullable UI ui;
    private @Nullable ScheduledFuture<?> tickTask;
    private @Nullable ScheduledFuture<?> highlightTask;

    public RefreshStaleDataView(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;

        addClassName("uc4-view");
        add(new H1("UC4 — Refresh stale data on return"));
        add(new Paragraph("The card below shows a fake USD/EUR exchange "
                + "rate. Switch away for more than 5 seconds and back — "
                + "the rate auto-refreshes and the card highlights. "
                + "Quick glances away (under 5 s) are ignored."));

        valueLabel.addClassName("rate-value");
        Span pair = new Span("USD/EUR");
        pair.addClassName("rate-pair");
        timestampLabel.addClassName("rate-timestamp");

        card.addClassName("rate-card");
        card.addThemeVariants(CardVariant.OUTLINED);
        card.setHeader(pair);
        card.add(valueLabel);
        card.addToFooter(timestampLabel);

        Button manual = new Button("Refresh now", e -> refresh());
        add(card, manual);

        renderRate();
        renderTimestamp();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI attachedUi = attachEvent.getUI();
        this.ui = attachedUi;

        Signal.effect(this, () -> {
            PageVisibility state = attachedUi.getPage().pageVisibilitySignal()
                    .get();
            handleTransition(prevState, state);
            prevState = state;
            if (state == PageVisibility.HIDDEN) {
                stopTimestampTick();
            } else {
                renderTimestamp();
                startTimestampTick();
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopTimestampTick();
        if (highlightTask != null) {
            highlightTask.cancel(false);
            highlightTask = null;
        }
        ui = null;
        super.onDetach(detachEvent);
    }

    private void handleTransition(PageVisibility from, PageVisibility to) {
        if (from != PageVisibility.HIDDEN && to == PageVisibility.HIDDEN) {
            hiddenAt = Instant.now();
        } else if (from == PageVisibility.HIDDEN
                && to == PageVisibility.VISIBLE) {
            Instant since = hiddenAt;
            hiddenAt = null;
            if (since != null && Duration.between(since, Instant.now())
                    .getSeconds() >= REFRESH_THRESHOLD_SECONDS) {
                refresh();
            }
        }
    }

    private void refresh() {
        rate = Math.max(0.5, Math.min(2.0, rate
                + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.01));
        updatedAt = Instant.now();
        renderRate();
        renderTimestamp();
        triggerHighlight();
    }

    private void renderRate() {
        valueLabel.setText("%.4f".formatted(rate));
    }

    private void renderTimestamp() {
        long secondsAgo = Duration.between(updatedAt, Instant.now())
                .getSeconds();
        timestampLabel.setText("Last updated " + secondsAgo + " s ago");
    }

    private void triggerHighlight() {
        card.addClassName("highlight");
        if (highlightTask != null) {
            highlightTask.cancel(false);
        }
        if (ui != null) {
            highlightTask = taskScheduler.schedule(ui
                    .accessLater(() -> card.removeClassName("highlight"), null),
                    Instant.now().plusMillis(600));
        }
    }

    private void startTimestampTick() {
        if (tickTask != null && !tickTask.isCancelled()) {
            return;
        }
        if (ui != null) {
            tickTask = taskScheduler.scheduleAtFixedRate(
                    ui.accessLater(this::renderTimestamp, null),
                    Instant.now().plus(Duration.ofSeconds(1)),
                    Duration.ofSeconds(1));
        }
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
        return rate;
    }

    void backdateHiddenAt(long secondsAgo) {
        this.hiddenAt = Instant.now().minusSeconds(secondsAgo);
    }
}
