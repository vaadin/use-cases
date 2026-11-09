package com.example.uc3;

import java.time.Duration;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.Geolocation;
import com.vaadin.flow.component.geolocation.GeolocationAvailability;
import com.vaadin.flow.component.geolocation.GeolocationOptions;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC3 — Locate returning visitors.
 * <p>
 * If the user has granted permission for this origin before, the view fetches
 * the location each time it is attached, without a click, so returning visitors
 * see location-based content at once. First-time visitors see the page without
 * a surprise permission prompt: they start the request with the button. The
 * hint at the top is bound to {@link Geolocation#availabilityHintSignal()} and
 * follows permission changes while the view is open.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Locate returning visitors")
@UseCaseDescription("Locating returning visitors without a surprise prompt")
@Menu(order = 3, title = "UC3 — Locate returning visitors")
public class AutoFetchView extends VerticalLayout {

    private final Span localContent = new Span("(no local content yet)");

    public AutoFetchView() {
        add(new H1("UC3 — Locate returning visitors"));
        add(new Paragraph("If you allowed this site to read your location "
                + "before, this view fetches it as soon as it opens. "
                + "Otherwise nothing happens until you click the button, "
                + "so a first-time visitor never gets a surprise "
                + "permission prompt. The first line shows the permission "
                + "state the browser reports, and follows changes while "
                + "the view is open."));

        Signal<GeolocationAvailability> availability = Geolocation
                .availabilityHintSignal();
        Span permissionHint = new Span();
        permissionHint.bindText(availability.map(AutoFetchView::hintFor));

        Button locate = new Button("Use my location", e -> fetchAndPopulate());
        add(permissionHint, locate, localContent);

        whenAttached(ui -> {
            // PROMPT, DENIED, UNKNOWN and UNSUPPORTED: nothing automatic.
            if (availability.peek() == GeolocationAvailability.GRANTED) {
                fetchAndPopulate();
            }
            return null;
        });
    }

    private static String hintFor(GeolocationAvailability availability) {
        return switch (availability) {
        case GRANTED -> "Location permission already granted.";
        case PROMPT -> "Location permission has not been asked for yet.";
        case DENIED -> "Location permission was denied for this site.";
        case UNSUPPORTED ->
            "Location is not available in this browser context.";
        case UNKNOWN ->
            "Location permission state is not known (Safari never reports it).";
        };
    }

    private void fetchAndPopulate() {
        GeolocationOptions opts = GeolocationOptions.builder()
                .timeout(Duration.ofSeconds(5))
                .maximumAge(Duration.ofMinutes(5)).build();
        Geolocation.getPosition(pos -> localContent
                .setText("Local content for lat=%.4f, lon=%.4f".formatted(
                        pos.coords().latitude(), pos.coords().longitude())),
                err -> localContent.setText(switch (err.errorCode()) {
                case PERMISSION_DENIED ->
                    "Could not auto-fetch: location permission denied.";
                case POSITION_UNAVAILABLE ->
                    "Could not auto-fetch: location unavailable.";
                case TIMEOUT -> "Could not auto-fetch: request timed out.";
                case UNKNOWN -> "Could not auto-fetch your location.";
                }), opts);
    }
}
