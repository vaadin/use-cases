package com.example.uc2;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC2 — Continuous tracking with reactive signal.
 * <p>
 * A Start/Stop toggle drives a single {@link GeolocationWatcher}. The watcher
 * is created lazily on the first click so a fresh page load does not start —
 * and immediately stop — a browser watch. Once created, the status text and the
 * toggle's label are both bound via {@code bindText} to computed signals over
 * the watcher's active and position signals plus an update counter. A single
 * effect handles the imperative side that bindings can't express: appending
 * each new reading to the grid and extending the path on the map. Resuming does
 * not clear the history — the grid and the map line keep growing.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Continuous tracking with reactive signal")
@UseCaseDescription("Following the user's position as it changes")
@Menu(order = 2, title = "UC2 — Tracking")
public class TrackingView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Span status = new Span("Idle");
    private final Button toggle = new Button("Start tracking");
    private final Grid<TrackPoint> history = new Grid<>(TrackPoint.class,
            false);
    private final List<TrackPoint> points = new ArrayList<>();

    private final Map map = new Map();
    private final List<Coordinate> pathCoords = new ArrayList<>();
    private @Nullable LineStringFeature pathLine;
    private @Nullable MarkerFeature startMarker;

    private @Nullable GeolocationWatcher watcher;
    private final ValueSignal<Boolean> hasUpdates = new ValueSignal<>(
            Boolean.FALSE);
    private final ValueSignal<Integer> updateCount = new ValueSignal<>(0);

    public TrackingView() {
        add(new H1("UC2 — Continuous tracking with reactive signal"));
        add(new Paragraph("Start the tracker to receive live updates "
                + "whenever the browser reports a new position. Each "
                + "reading is appended to the history and extends the "
                + "path on the map. Resuming after Stop keeps the "
                + "accumulated history."));

        toggle.addClickListener(e -> toggleTracking());
        add(toggle, status);

        map.setHeight("400px");
        map.setWidthFull();
        add(map);

        add(new H2("Position history"));
        configureGrid();
        add(history);
    }

    private void configureGrid() {
        history.addColumn(TrackPoint::index).setHeader("#").setAutoWidth(true);
        history.addColumn(p -> TIME.format(Instant.ofEpochMilli(p.timestamp())))
                .setHeader("Time").setAutoWidth(true);
        history.addColumn(p -> "%.5f".formatted(p.latitude()))
                .setHeader("Latitude").setAutoWidth(true);
        history.addColumn(p -> "%.5f".formatted(p.longitude()))
                .setHeader("Longitude").setAutoWidth(true);
        history.addColumn(p -> "%.0f m".formatted(p.accuracy()))
                .setHeader("Accuracy").setAutoWidth(true);
        history.setItems(points);
        history.setAllRowsVisible(true);
    }

    private void toggleTracking() {
        if (watcher == null) {
            ensureWatcher();
            return;
        }
        if (watcher.activeSignal().peek()) {
            watcher.stop();
        } else {
            watcher.resume();
        }
    }

    private void ensureWatcher() {
        GeolocationOptions options = GeolocationOptions.builder()
                .highAccuracy(true).maximumAge(Duration.ZERO).build();
        GeolocationWatcher w = Geolocation.watchPosition(this, options);
        watcher = w;

        status.bindText(Signal.computed(() -> {
            if (!w.activeSignal().get() && hasUpdates.get()) {
                return "Stopped after " + updateCount.get() + " updates";
            }
            return switch (w.positionSignal().get()) {
            case GeolocationPending p -> "Waiting for first reading…";
            case GeolocationPosition pos -> "Update #" + updateCount.get();
            case GeolocationError err -> switch (err.errorCode()) {
            case PERMISSION_DENIED -> "Location permission was denied.";
            case POSITION_UNAVAILABLE -> "Could not determine your location.";
            case TIMEOUT -> "Location request timed out.";
            case UNKNOWN -> "Could not get your location.";
            };
            };
        }));

        Signal.effect(this, () -> {
            if (w.positionSignal().get() instanceof GeolocationPosition pos) {
                appendPosition(pos);
            }
        });

        toggle.bindText(Signal.computed(() -> {
            boolean active = w.activeSignal().get();
            if (active) {
                return "Stop tracking";
            }
            return hasUpdates.get() ? "Resume tracking" : "Start tracking";
        }));
    }

    private void appendPosition(GeolocationPosition pos) {
        int next = updateCount.peek() + 1;
        updateCount.set(next);
        hasUpdates.set(Boolean.TRUE);
        points.add(
                new TrackPoint(next, pos.timestamp(), pos.coords().latitude(),
                        pos.coords().longitude(), pos.coords().accuracy()));
        history.getDataProvider().refreshAll();

        Coordinate c = new Coordinate(pos.coords().longitude(),
                pos.coords().latitude());
        pathCoords.add(c);
        map.setCenter(c);
        if (pathCoords.size() == 1) {
            map.setZoom(15);
            startMarker = new MarkerFeature(c);
            map.getFeatureLayer().addFeature(startMarker);
        } else if (pathLine == null) {
            pathLine = new LineStringFeature(
                    pathCoords.toArray(Coordinate[]::new));
            Style lineStyle = new Style();
            lineStyle.setStroke(new Stroke("#1976d2", 4));
            pathLine.setStyle(lineStyle);
            map.getFeatureLayer().addFeature(pathLine);
        } else {
            pathLine.setCoordinates(pathCoords.toArray(Coordinate[]::new));
        }
    }

    private record TrackPoint(int index, long timestamp, double latitude,
            double longitude, double accuracy) {
    }
}
