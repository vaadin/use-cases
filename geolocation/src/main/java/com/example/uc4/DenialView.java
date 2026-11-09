package com.example.uc4;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.geolocation.Geolocation;
import com.vaadin.flow.component.geolocation.GeolocationAvailability;
import com.vaadin.flow.component.geolocation.GeolocationCoordinates;
import com.vaadin.flow.component.geolocation.GeolocationError;
import com.vaadin.flow.component.geolocation.GeolocationErrorCode;
import com.vaadin.flow.component.geolocation.GeolocationPosition;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC4 — When location fails.
 * <p>
 * Each scenario is in its own card so you can tell what you are testing. The
 * first two cards simulate what the browser reports, so you can try every
 * branch without changing site permissions: they pass the chosen value to the
 * same handlers a real request would use. The "Real browser request" card calls
 * the actual API and shows the live
 * {@link Geolocation#availabilityHintSignal()}.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — When location fails")
@UseCaseDescription("Handling a denied, failed or unavailable location")
@Menu(order = 4, title = "UC4 — When location fails")
public class DenialView extends VerticalLayout {

    public DenialView() {
        add(new H1("UC4 — When location fails"));
        add(new Paragraph("The app adapts to two kinds of failure: an "
                + "availability that says location can not be used at "
                + "all or is blocked, and an error returned by a single "
                + "location request. The first two cards simulate what "
                + "the browser reports; the last one asks the real "
                + "browser."));

        add(new AvailabilityCard());
        add(new RequestOutcomeCard());
        add(new RealBrowserCard());
    }

    // ------------------------------------------------------------------
    // Card 1 — availability-driven rendering
    // ------------------------------------------------------------------

    /**
     * Demonstrates how the app renders based on the current
     * {@link GeolocationAvailability} reported by the browser.
     */
    private static class AvailabilityCard extends Card {

        private final Button locate = new Button("Find stores near me");
        private final TextField postcode = new TextField("Postcode");
        private final Span hint = new Span();

        AvailabilityCard() {
            addThemeVariants(CardVariant.OUTLINED);
            setTitle("Availability → rendering");

            add(new Paragraph("Pick what the browser reports as the "
                    + "current availability. The preview below shows how "
                    + "the store-finder view would render in that case."));

            Select<GeolocationAvailability> choose = new Select<>();
            choose.setLabel("Browser-reported availability");
            choose.setItems(GeolocationAvailability.values());
            choose.addValueChangeListener(e -> applyAvailability(e.getValue()));
            add(choose);

            add(new H3("Preview"));
            postcode.setVisible(false);
            add(hint, locate, postcode);

            // Default to PROMPT so the preview shows the "normal" branch.
            choose.setValue(GeolocationAvailability.PROMPT);
        }

        private void applyAvailability(GeolocationAvailability a) {
            locate.setVisible(true);
            postcode.setVisible(false);
            switch (a) {
            case GRANTED -> hint.setText(
                    "Permission already granted — clicking the button is silent.");
            case PROMPT -> hint.setText(
                    "Permission will be requested the first time the button is clicked.");
            case UNKNOWN -> hint.setText(
                    "Browser does not report the state — wait for explicit user action.");
            case DENIED -> {
                postcode.setVisible(true);
                hint.setText(
                        "Location is blocked for this site. Click the padlock "
                                + "in the address bar to re-enable, or enter a "
                                + "postcode below.");
            }
            case UNSUPPORTED -> {
                locate.setVisible(false);
                postcode.setVisible(true);
                hint.setText("Geolocation is not available in this context. "
                        + "Enter a postcode to find nearby stores.");
            }
            }
        }

    }

    // ------------------------------------------------------------------
    // Card 2 — request-error handling
    // ------------------------------------------------------------------

    /**
     * Demonstrates how the app reacts to each possible outcome of a single
     * {@code get()} request. The selector picks what the client would return;
     * the button runs the handler.
     */
    private static class RequestOutcomeCard extends Card {

        private enum Outcome {
            SUCCESS("A position (59.437, 24.7535)"),
            PERMISSION_DENIED("Error — permission denied"),
            POSITION_UNAVAILABLE("Error — position unavailable"),
            TIMEOUT("Error — request timed out"),
            UNKNOWN_CODE("Error — unknown future code (99)");

            private final String label;

            Outcome(String label) {
                this.label = label;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        private final Span output = new Span();

        RequestOutcomeCard() {
            addThemeVariants(CardVariant.OUTLINED);
            setTitle("Request outcome → rendering");

            add(new Paragraph(
                    "Pick the result the browser should return, then click "
                            + "Run request. The handler writes the "
                            + "user-facing message below."));

            Select<Outcome> choose = new Select<>();
            choose.setLabel("What the request returns");
            choose.setItems(Outcome.values());
            choose.setValue(Outcome.PERMISSION_DENIED);
            add(choose);

            Button run = new Button("Run request",
                    e -> simulate(choose.getValue()));
            add(run);

            add(new H3("Message shown to the user"));
            add(output);
        }

        /**
         * Passes the chosen outcome to the same two handlers that
         * {@code Geolocation.getPosition(onSuccess, onError)} would call.
         */
        private void simulate(Outcome outcome) {
            switch (outcome) {
            case SUCCESS -> onSuccess(
                    new GeolocationPosition(
                            new GeolocationCoordinates(59.437, 24.7535, 10.0,
                                    null, null, null, null),
                            System.currentTimeMillis()));
            case PERMISSION_DENIED -> onError(new GeolocationError(
                    GeolocationErrorCode.PERMISSION_DENIED.code(),
                    "Simulated: user denied geolocation"));
            case POSITION_UNAVAILABLE -> onError(new GeolocationError(
                    GeolocationErrorCode.POSITION_UNAVAILABLE.code(),
                    "Simulated: position unavailable"));
            case TIMEOUT -> onError(
                    new GeolocationError(GeolocationErrorCode.TIMEOUT.code(),
                            "Simulated: request timed out"));
            case UNKNOWN_CODE -> onError(
                    new GeolocationError(99, "Simulated: unknown future code"));
            }
        }

        private void onSuccess(GeolocationPosition pos) {
            output.setText("Stores near lat=%.4f, lon=%.4f".formatted(
                    pos.coords().latitude(), pos.coords().longitude()));
        }

        private void onError(GeolocationError err) {
            // An unknown code maps to GeolocationErrorCode.UNKNOWN.
            output.setText(switch (err.errorCode()) {
            case PERMISSION_DENIED ->
                "Location not shared. Please enter a postcode.";
            case POSITION_UNAVAILABLE -> "We couldn't determine your location.";
            case TIMEOUT -> "Location request timed out. Please try again.";
            case UNKNOWN -> "Could not get your location.";
            });
        }

    }

    // ------------------------------------------------------------------
    // Card 3 — real browser request
    // ------------------------------------------------------------------

    /**
     * Runs the real {@code Geolocation.getPosition()} against the real browser
     * so you can verify end-to-end behaviour once the simulations look right.
     */
    private static class RealBrowserCard extends Card {

        private final Span output = new Span("(no request run yet)");

        RealBrowserCard() {
            addThemeVariants(CardVariant.OUTLINED);
            setTitle("Real browser request");

            add(new Paragraph("Calls the real API. The outcome depends on your "
                    + "browser's current permission state."));

            // Bound to the availability signal, so the label follows
            // permission changes (granted → denied, prompt → granted) instead
            // of showing the value from when the view opened.
            Span availabilityLabel = new Span();
            availabilityLabel.bindText(Geolocation.availabilityHintSignal()
                    .map(a -> "Current availability: " + a));
            add(availabilityLabel);

            Button locate = new Button("Use my location", e -> runReal());
            add(locate);

            add(new H3("Response"));
            add(output);
        }

        private void runReal() {
            Geolocation.getPosition(pos -> output
                    .setText("Position: lat=%.5f, lon=%.5f (±%.0f m)".formatted(
                            pos.coords().latitude(), pos.coords().longitude(),
                            pos.coords().accuracy())),
                    err -> output.setText(switch (err.errorCode()) {
                    case PERMISSION_DENIED -> "Location permission was denied.";
                    case POSITION_UNAVAILABLE ->
                        "Could not determine your location.";
                    case TIMEOUT -> "Location request timed out.";
                    case UNKNOWN -> "Could not get your location.";
                    }));
        }

    }
}
