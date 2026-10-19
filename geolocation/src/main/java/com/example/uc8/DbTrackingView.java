package com.example.uc8;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.Geolocation;
import com.vaadin.flow.component.geolocation.GeolocationError;
import com.vaadin.flow.component.geolocation.GeolocationOptions;
import com.vaadin.flow.component.geolocation.GeolocationPosition;
import com.vaadin.flow.component.geolocation.GeolocationResult;
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
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC8 — Route in a database.
 * <p>
 * Like UC2, but the database is the source of truth for what is shown. A
 * listener registered with {@link GeolocationWatcher#addPositionListener} saves
 * each new {@link GeolocationPosition} through {@link TrackedPositionService}.
 * Only then are the grid and the map line refreshed, by reading the rows of
 * this browser session back from the database. The rows are kept per Vaadin
 * session, so a reader who leaves the view and comes back sees the stored route
 * again. "Clear history" deletes the session's rows and the view follows.
 * <p>
 * After a denied permission the watcher still reports itself as active (see
 * API-GAPS.md #1).
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Route in a database")
@UseCaseDescription("Storing a tracked route in a database")
@Menu(order = 8, title = "UC8 — Route in a database")
public class DbTrackingView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final TrackedPositionService service;
    private final String trackId = TrackId.of(VaadinSession.getCurrent());

    private final Grid<TrackedPosition> history = new Grid<>(
            TrackedPosition.class, false);
    private final Map map = new Map();

    // The number of rows stored for this session, set from the database.
    private final ValueSignal<Integer> storedCount = new ValueSignal<>(0);

    public DbTrackingView(TrackedPositionService service) {
        this.service = service;

        add(new H1("UC8 — Route in a database"));
        add(new Paragraph("Each location the browser reports is first "
                + "saved to the database. Only then are the grid and the "
                + "map line refreshed, by reading the database again. The "
                + "database, not the tracker signal, drives what you see: "
                + "leave the view and come back, and your route is still "
                + "there. \"Clear history\" deletes the rows of your "
                + "browser session and the view follows. If you block "
                + "location while tracking, the error shows but Stop "
                + "tracking stays visible: click it, then Start again."));

        GeolocationOptions options = GeolocationOptions.builder()
                .highAccuracy(true).maximumAge(Duration.ZERO).build();
        GeolocationWatcher tracker = Geolocation.watchPosition(this, options);
        // A new watcher starts on the first attach. Stop it here, so the
        // browser does not ask for the location before the user clicks Start.
        tracker.stop();
        tracker.addPositionListener(this::persistAndRefresh,
                DbTrackingView::showErrorNotification);

        Button start = new Button("", e -> tracker.resume());
        start.bindText(storedCount.map(
                count -> count > 0 ? "Resume tracking" : "Start tracking"));
        start.bindVisible(tracker.activeSignal().map(active -> !active));
        Button stop = new Button("Stop tracking", e -> tracker.stop());
        stop.bindVisible(tracker.activeSignal());
        Button clear = new Button("Clear history", e -> clearHistory());

        Span status = new Span();
        status.bindText(
                Signal.computed(() -> statusText(tracker.activeSignal().get(),
                        tracker.positionSignal().get(), storedCount.get())));

        add(new HorizontalLayout(start, stop, clear), status);

        map.setHeight("400px");
        map.setWidthFull();
        add(map);

        add(new H2("Position history (read from the database)"));
        configureGrid();
        add(history);

        refreshFromDb();
    }

    private static String statusText(boolean active, GeolocationResult result,
            int stored) {
        if (result instanceof GeolocationError err) {
            return errorText(err);
        }
        if (!active) {
            return stored > 0
                    ? "Not tracking. Positions stored: " + stored + "."
                    : "Idle";
        }
        return result instanceof GeolocationPosition
                ? "Saved position #" + stored + " to the database"
                : "Waiting for first reading…";
    }

    private static String errorText(GeolocationError error) {
        return switch (error.errorCode()) {
        case PERMISSION_DENIED -> "Location permission was denied.";
        case POSITION_UNAVAILABLE -> "Could not determine your location.";
        case TIMEOUT -> "Location request timed out.";
        case UNKNOWN -> "Could not get your location.";
        };
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

    private static void showErrorNotification(GeolocationError error) {
        Notification n = Notification.show(errorText(error), 5000,
                Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.ERROR);
    }

    private void persistAndRefresh(GeolocationPosition pos) {
        service.save(trackId, pos.timestamp(), pos.coords().latitude(),
                pos.coords().longitude(), pos.coords().accuracy());
        refreshFromDb();
    }

    private void clearHistory() {
        service.clearSession(trackId);
        refreshFromDb();
    }

    private void refreshFromDb() {
        List<TrackedPosition> rows = service.findBySession(trackId);
        history.setItems(rows);
        storedCount.set(rows.size());

        map.getFeatureLayer().removeAllFeatures();
        if (rows.isEmpty()) {
            return;
        }

        Coordinate[] coords = rows.stream()
                .map(p -> new Coordinate(p.getLongitude(), p.getLatitude()))
                .toArray(Coordinate[]::new);
        map.getFeatureLayer().addFeature(new MarkerFeature(coords[0]));
        if (coords.length >= 2) {
            LineStringFeature pathLine = new LineStringFeature(coords);
            Style lineStyle = new Style();
            // The map draws on a canvas, which can not read CSS custom
            // properties such as --aura-accent-color.
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

    /**
     * The key of the stored rows: one per Vaadin session, so the route survives
     * leaving the view and coming back.
     */
    private record TrackId(String value) {

        static String of(VaadinSession session) {
            TrackId id = session.getAttribute(TrackId.class);
            if (id == null) {
                id = new TrackId(UUID.randomUUID().toString());
                session.setAttribute(TrackId.class, id);
            }
            return id.value();
        }
    }
}
