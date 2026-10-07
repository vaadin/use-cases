package com.example.uc5;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.GeolocationCoordinates;
import com.vaadin.flow.component.geolocation.GeolocationPosition;
import com.vaadin.flow.component.geolocation.GeolocationSimulator;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(classes = DetailedDataView.class)
class DetailedDataViewTest extends BrowserlessTest {

    @Test
    void positionWithFullCoords_rendersAllFields() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        GeolocationCoordinates coords = new GeolocationCoordinates(51.5074,
                -0.1278, 10.0, 12.5, 3.0, 90.0, 1.5);
        geolocation
                .setLocation(new GeolocationPosition(coords, 1700000000000L));

        navigate(DetailedDataView.class);

        Button fetch = find(Button.class).withText("Read full position")
                .single();
        test(fetch).click();

        assertTrue(find(Span.class).withTextContaining("51.507400°").exists(),
                "Latitude field should be rendered");
        assertTrue(find(Span.class).withTextContaining("-0.127800°").exists(),
                "Longitude field should be rendered");
        assertTrue(find(Span.class).withTextContaining("10.0 m").exists(),
                "Accuracy field should be rendered");
        assertTrue(find(Span.class).withTextContaining("12.5 m").exists(),
                "Altitude field should be rendered");
        assertTrue(find(Span.class).withTextContaining("3.0 m").exists(),
                "Altitude accuracy field should be rendered");
        assertTrue(find(Span.class).withTextContaining("90.0°").exists(),
                "Heading field should be rendered");
        assertTrue(find(Span.class).withTextContaining("1.50 m/s").exists(),
                "Speed field should be rendered");
    }

    @Test
    void positionWithNullExtras_rendersDashesForMissingFields() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        geolocation.setLocation(48.8566, 2.3522, 30.0);

        navigate(DetailedDataView.class);

        Button fetch = find(Button.class).withText("Read full position")
                .single();
        test(fetch).click();

        assertTrue(find(Span.class).withTextContaining("48.856600°").exists(),
                "Latitude field should be rendered");

        int dashCount = find(Span.class).withText("—").all().size();
        assertTrue(dashCount >= 4,
                "At least 4 fields should be shown as '—' for null coordinates, found: "
                        + dashCount);
    }
}
