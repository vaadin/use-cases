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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC2 — Live tracking.
 * <p>
 * A Start/Stop toggle drives one {@link GeolocationWatcher}. The watcher is
 * created in the constructor and stopped at once, so opening the view does not
 * start a browser watch before the user asks for it. The status text and the
 * toggle's label are bound to computed signals over the watcher's
 * {@link GeolocationWatcher#activeSignal() active} and
 * {@link GeolocationWatcher#positionSignal() position} signals. An effect adds
 * each new reading to the history grid and extends the path on the map.
 * Resuming does not clear the history: the grid and the map line keep growing.
 * <p>
 * After a denied permission the watcher still reports itself as active (see
 * API-GAPS.md #1).
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Live tracking")
@UseCaseDescription("Following the user's position as it changes")
@Menu(order = 2, title = "UC2 — Live tracking")
public class TrackingView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Grid<TrackPoint> history = new Grid<>(TrackPoint.class,
            false);
    private final List<TrackPoint> points = new ArrayList<>();
    private final ValueSignal<Integer> updateCount = new ValueSignal<>(0);

    private final Map map = new Map();
    private final List<Coordinate> pathCoords = new ArrayList<>();
    private @Nullable LineStringFeature pathLine;

    public TrackingView() {
        add(new H1("UC2 — Live tracking"));
        add(new Paragraph("Start tracking to get a new reading each time "
                + "the browser reports a new position. Each reading is "
                + "added to the history and extends the path on the map. "
                + "Stopping and resuming keeps the history. If you block "
                + "location while tracking, the error shows but the button "
                + "still says Stop tracking: click Stop, then Start again."));

        GeolocationOptions options = GeolocationOptions.builder()
                .highAccuracy(true).maximumAge(Duration.ZERO).build();
        GeolocationWatcher watcher = Geolocation.watchPosition(this, options);
        // A new watcher starts on the first attach. Stop it here, so the
        // browser does not ask for the location before the user clicks Start.
        watcher.stop();

        Button toggle = new Button("", e -> {
            if (watcher.activeSignal().peek()) {
                watcher.stop();
            } else {
                watcher.resume();
            }
        });
        toggle.bindText(Signal.computed(() -> {
            if (watcher.activeSignal().get()) {
                return "Stop tracking";
            }
            return updateCount.get() > 0 ? "Resume tracking" : "Start tracking";
        }));

        Span status = new Span();
        status.bindText(
                Signal.computed(() -> statusText(watcher.activeSignal().get(),
                        watcher.positionSignal().get(), updateCount.get())));

        Signal.effect(this, () -> {
            if (watcher.positionSignal()
                    .get() instanceof GeolocationPosition pos) {
                appendPosition(pos);
            }
        });

        add(toggle, status);

        map.setHeight("400px");
        map.setWidthFull();
        add(map);

        add(new H2("Position history"));
        configureGrid();
        add(history);
    }

    private static String statusText(boolean active, GeolocationResult result,
            int updates) {
        if (result instanceof GeolocationError err) {
            return switch (err.errorCode()) {
            case PERMISSION_DENIED -> "Location permission was denied.";
            case POSITION_UNAVAILABLE -> "Could not determine your location.";
            case TIMEOUT -> "Location request timed out.";
            case UNKNOWN -> "Could not get your location.";
            };
        }
        if (!active) {
            if (updates == 0) {
                return "Idle";
            }
            return updates == 1 ? "Stopped after 1 update"
                    : "Stopped after " + updates + " updates";
        }
        return result instanceof GeolocationPosition ? "Update #" + updates
                : "Waiting for first reading…";
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

    private void appendPosition(GeolocationPosition pos) {
        int next = updateCount.peek() + 1;
        updateCount.set(next);
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
            map.getFeatureLayer().addFeature(new MarkerFeature(c));
        } else if (pathLine == null) {
            pathLine = new LineStringFeature(
                    pathCoords.toArray(Coordinate[]::new));
            Style lineStyle = new Style();
            // The map draws on a canvas, which can not read CSS custom
            // properties such as --aura-accent-color.
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
