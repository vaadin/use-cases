package com.example.uc8;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.Geolocation;
import com.vaadin.flow.component.geolocation.GeolocationError;
import com.vaadin.flow.component.geolocation.GeolocationOptions;
import com.vaadin.flow.component.geolocation.GeolocationPending;
import com.vaadin.flow.component.geolocation.GeolocationPosition;
import com.vaadin.flow.component.geolocation.GeolocationWatcher;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.map.Map;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.map.configuration.feature.LineStringFeature;
import com.vaadin.flow.component.map.configuration.feature.MarkerFeature;
import com.vaadin.flow.component.map.configuration.style.Stroke;
import com.vaadin.flow.component.map.configuration.style.Style;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC8 — DB-backed continuous tracking.
 * <p>
 * Like UC2 but the database is the source of truth for the rendered data. A
 * position listener registered via
 * {@code GeolocationWatcher.addPositionListener} persists each new
 * {@link GeolocationPosition} via {@link TrackedPositionService}, and only then
 * is the grid and the map line refreshed by re-reading the rows for this view's
 * session id from the database. A "Clear history" button deletes the session's
 * rows and triggers the same DB-driven refresh, so the UI mirrors whatever the
 * database currently holds.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — DB-backed tracking")
@UseCaseDescription("Storing a tracked route in a database")
@Menu(order = 8, title = "UC8 — DB-backed tracking")
public class DbTrackingView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final TrackedPositionService service;
    private final String sessionId = UUID.randomUUID().toString();

    private final Span status = new Span("Idle");
    private final Button start = new Button("Start tracking");
    private final Button stop = new Button("Stop tracking");
    private final Button clear = new Button("Clear history");
    private final Grid<TrackedPosition> history = new Grid<>(
            TrackedPosition.class, false);

    private final Map map = new Map();
    private @Nullable LineStringFeature pathLine;
    private @Nullable MarkerFeature startMarker;

    private @Nullable GeolocationWatcher tracker;
    private final ValueSignal<Boolean> hasUpdates = new ValueSignal<>(
            Boolean.FALSE);
    private final ValueSignal<Integer> updateCount = new ValueSignal<>(0);

    public DbTrackingView(TrackedPositionService service) {
        this.service = service;

        add(new H1("UC8 — DB-backed tracking"));
        add(new Paragraph("Each location reported by the browser is first "
                + "saved to the database, and only then the grid and the "
                + "map line are refreshed by re-reading the database. The "
                + "database — not the tracker signal — drives what you "
                + "see. \"Clear history\" deletes this session's rows and "
                + "the UI follows."));

        start.addClickListener(e -> startTracking());
        stop.addClickListener(e -> stopTracking());
        stop.setVisible(false);
        clear.addClickListener(e -> clearHistory());
        add(new HorizontalLayout(start, stop, clear), status);

        map.setHeight("400px");
        map.setWidthFull();
        add(map);

        add(new H2("Position history (loaded from database)"));
        configureGrid();
        add(history);

        refreshFromDb();
    }

    private void configureGrid() {
        history.addColumn(
                p -> TIME.format(Instant.ofEpochMilli(p.getTimestamp())))
                .setHeader("Time").setAutoWidth(true);
        history.addColumn(p -> "%.5f".formatted(p.getLatitude()))
                .setHeader("Latitude").setAutoWidth(true);
        history.addColumn(p -> "%.5f".formatted(p.getLongitude()))
                .setHeader("Longitude").setAutoWidth(true);
        history.addColumn(p -> "%.0f m".formatted(p.getAccuracy()))
                .setHeader("Accuracy").setAutoWidth(true);
        history.setAllRowsVisible(true);
    }

    private void startTracking() {
        if (tracker == null) {
            ensureTracker();
        } else {
            tracker.resume();
        }
    }

    private void stopTracking() {
        if (tracker != null) {
            tracker.stop();
        }
    }

    private void ensureTracker() {
        GeolocationOptions options = GeolocationOptions.builder()
                .highAccuracy(true).maximumAge(Duration.ZERO).build();
        GeolocationWatcher t = Geolocation.watchPosition(this, options);
        tracker = t;

        status.bindText(Signal.computed(() -> {
            if (!t.activeSignal().get() && hasUpdates.get()) {
                return "Stopped after " + updateCount.get() + " updates";
            }
            return switch (t.positionSignal().get()) {
            case GeolocationPending p -> "Waiting for first reading…";
            case GeolocationPosition pos ->
                "Saved update #" + updateCount.get() + " to DB";
            case GeolocationError err -> switch (err.errorCode()) {
            case PERMISSION_DENIED -> "Location permission was denied.";
            case POSITION_UNAVAILABLE -> "Could not determine your location.";
            case TIMEOUT -> "Location request timed out.";
            case UNKNOWN -> "Could not get your location.";
            };
            };
        }));

        t.addPositionListener(this::persistAndRefresh,
                this::showErrorNotification);

        start.bindVisible(Signal
                .computed(() -> Boolean.valueOf(!t.activeSignal().get())));
        stop.bindVisible(t.activeSignal());
        start.bindText(Signal.computed(
                () -> hasUpdates.get() ? "Resume tracking" : "Start tracking"));
    }

    private void showErrorNotification(GeolocationError error) {
        String description = switch (error.errorCode()) {
        case PERMISSION_DENIED -> "Location permission was denied.";
        case POSITION_UNAVAILABLE -> "Could not determine your location.";
        case TIMEOUT -> "Location request timed out.";
        case UNKNOWN -> "Could not get your location.";
        };
        Notification n = Notification.show(description, 5000,
                Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.ERROR);
    }

    private void persistAndRefresh(GeolocationPosition pos) {
        service.save(sessionId, pos.timestamp(), pos.coords().latitude(),
                pos.coords().longitude(), pos.coords().accuracy());
        updateCount.set(updateCount.peek() + 1);
        hasUpdates.set(Boolean.TRUE);
        refreshFromDb();
    }

    private void clearHistory() {
        service.clearSession(sessionId);
        updateCount.set(0);
        hasUpdates.set(Boolean.FALSE);
        refreshFromDb();
    }

    private void refreshFromDb() {
        List<TrackedPosition> rows = service.findBySession(sessionId);
        history.setItems(rows);

        map.getFeatureLayer().removeAllFeatures();
        pathLine = null;
        startMarker = null;
        if (rows.isEmpty()) {
            return;
        }

        Coordinate[] coords = rows.stream()
                .map(p -> new Coordinate(p.getLongitude(), p.getLatitude()))
                .toArray(Coordinate[]::new);
        startMarker = new MarkerFeature(coords[0]);
        map.getFeatureLayer().addFeature(startMarker);
        if (coords.length >= 2) {
            pathLine = new LineStringFeature(coords);
            Style lineStyle = new Style();
            lineStyle.setStroke(new Stroke("#1976d2", 4));
            pathLine.setStyle(lineStyle);
            map.getFeatureLayer().addFeature(pathLine);
        }
        Coordinate latest = coords[coords.length - 1];
        map.setCenter(latest);
        if (map.getZoom() < 15) {
            map.setZoom(15);
        }
    }
}
