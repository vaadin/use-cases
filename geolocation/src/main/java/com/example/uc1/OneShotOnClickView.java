package com.example.uc1;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.Geolocation;
import com.vaadin.flow.component.geolocation.GeolocationError;
import com.vaadin.flow.component.geolocation.GeolocationPosition;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.map.Map;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.map.configuration.feature.MarkerFeature;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — One-shot request on user click.
 * <p>
 * Classic "Use my location" button. The click handler calls
 * {@code Geolocation.getPosition(...)} and reacts to either the returned
 * {@link GeolocationPosition} or a {@link GeolocationError}. On success the map
 * below is centered on the result and a marker is dropped.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — One-shot request on user click")
@UseCaseDescription("Asking for the user's location with a button")
@Menu(order = 1, title = "UC1 — One-shot request")
public class OneShotOnClickView extends VerticalLayout {

    public OneShotOnClickView() {
        add(new H1("UC1 — One-shot request on user click"));
        add(new Paragraph(
                "Click the button below to request the current position once."));

        Span result = new Span("(no reading yet)");

        Map map = new Map();
        map.setHeight("400px");
        map.setWidthFull();

        Button locate = new Button("Use my location", e -> {
            Geolocation.getPosition(pos -> {
                result.setText("lat=%.5f, lon=%.5f (±%.0f m)".formatted(
                        pos.coords().latitude(), pos.coords().longitude(),
                        pos.coords().accuracy()));
                Coordinate c = new Coordinate(pos.coords().longitude(),
                        pos.coords().latitude());
                map.setCenter(c);
                if (map.getZoom() < 14) {
                    map.setZoom(14);
                }
                map.getFeatureLayer().removeAllFeatures();
                map.getFeatureLayer().addFeature(new MarkerFeature(c));
            }, err -> result.setText(switch (err.errorCode()) {
            case PERMISSION_DENIED -> "Location permission was denied.";
            case POSITION_UNAVAILABLE -> "Could not determine your location.";
            case TIMEOUT -> "Location request timed out.";
            case UNKNOWN -> "Could not get your location.";
            }));
        });

        add(locate, result, map);
    }
}
