package com.example.uc5;

import java.time.Duration;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.Geolocation;
import com.vaadin.flow.component.geolocation.GeolocationCoordinates;
import com.vaadin.flow.component.geolocation.GeolocationOptions;
import com.vaadin.flow.component.geolocation.GeolocationPosition;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC5 — Reading detailed position data.
 * <p>
 * Dumps every field reported by the browser. Fields that the device cannot
 * measure (altitude, heading, speed on a laptop without GPS) arrive as
 * {@code null} and are shown as "—".
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Reading detailed position data")
@UseCaseDescription("Reading altitude, heading, speed and accuracy")
@Menu(order = 5, title = "UC5 — Detailed data")
public class DetailedDataView extends VerticalLayout {

    public DetailedDataView() {
        add(new H1("UC5 — Detailed position data"));
        add(new Paragraph(
                "Request a high-accuracy reading and display all fields."));

        Div output = new Div();
        Button fetch = new Button("Read full position", e -> {
            output.removeAll();
            GeolocationOptions opts = GeolocationOptions.builder()
                    .highAccuracy(true).timeout(Duration.ofSeconds(10)).build();
            Geolocation.getPosition(pos -> renderPosition(output, pos),
                    err -> output.add(new Span(switch (err.errorCode()) {
                    case PERMISSION_DENIED -> "Location permission was denied.";
                    case POSITION_UNAVAILABLE ->
                        "Could not determine your location.";
                    case TIMEOUT -> "Location request timed out.";
                    case UNKNOWN -> "Could not get your location.";
                    })), opts);
        });
        add(fetch, output);
    }

    private void renderPosition(Div output, GeolocationPosition pos) {
        GeolocationCoordinates c = pos.coords();
        output.add(field("latitude", "%.6f°".formatted(c.latitude())));
        output.add(field("longitude", "%.6f°".formatted(c.longitude())));
        output.add(field("accuracy", "%.1f m".formatted(c.accuracy())));
        output.add(field("altitude",
                c.altitude() == null ? "—" : "%.1f m".formatted(c.altitude())));
        output.add(field("altitude accuracy", c.altitudeAccuracy() == null ? "—"
                : "%.1f m".formatted(c.altitudeAccuracy())));
        output.add(field("heading",
                c.heading() == null ? "—" : "%.1f°".formatted(c.heading())));
        output.add(field("speed",
                c.speed() == null ? "—" : "%.2f m/s".formatted(c.speed())));
        output.add(field("timestamp", pos.timestampAsInstant().toString()));
    }

    private Div field(String label, String value) {
        Div row = new Div();
        row.add(new Span(label + ": "));
        row.add(new Span(value));
        return row;
    }
}
