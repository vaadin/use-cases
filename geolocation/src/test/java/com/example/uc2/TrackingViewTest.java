package com.example.uc2;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.GeolocationErrorCode;
import com.vaadin.flow.component.geolocation.GeolocationSimulator;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(classes = TrackingView.class)
class TrackingViewTest extends BrowserlessTest {

    @Test
    void openingTheView_doesNotStartAWatch() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        navigate(TrackingView.class);

        assertTrue(geolocation.activeTrackers().isEmpty(),
                "No browser watch should run before the user clicks Start");
        assertTrue(find(Span.class).withText("Idle").exists());
    }

    @Test
    void stopAndResume_keepsTheHistory() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        navigate(TrackingView.class);
        Grid<?> history = find(Grid.class).single();

        test(find(Button.class).withText("Start tracking").single()).click();
        geolocation.setLocation(60.1699, 24.9384, 15.0);
        test(find(Button.class).withText("Stop tracking").single()).click();

        assertTrue(geolocation.activeTrackers().isEmpty(),
                "Stop should end the browser watch");
        assertTrue(
                find(Span.class).withText("Stopped after 1 update").exists());

        test(find(Button.class).withText("Resume tracking").single()).click();
        geolocation.setLocation(60.1700, 24.9385, 12.0);

        assertEquals(2, test(history).size(),
                "Resuming should add to the history, not replace it");
    }

    @Test
    void startTracking_userMoves_growsHistoryAndUpdatesStatus() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        navigate(TrackingView.class);

        Button toggle = find(Button.class).withText("Start tracking").single();
        test(toggle).click();

        assertEquals(1, geolocation.activeTrackers().size(),
                "Exactly one tracker session should be active after starting");

        geolocation.setLocation(60.1699, 24.9384, 15.0);
        Span status = find(Span.class).withText("Update #1").single();
        assertEquals("Update #1", status.getText());

        geolocation.setLocation(60.1700, 24.9385, 12.0);
        status = find(Span.class).withText("Update #2").single();
        assertEquals("Update #2", status.getText());
    }

    @Test
    void positionUnavailableMidTrack_displaysErrorStatus() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        navigate(TrackingView.class);

        Button toggle = find(Button.class).withText("Start tracking").single();
        test(toggle).click();

        geolocation.setUnavailable(GeolocationErrorCode.POSITION_UNAVAILABLE,
                "position unavailable");

        Span status = find(Span.class).withTextContaining("Could not determine")
                .single();
        assertTrue(status.getText().contains("location"),
                "Status should describe the location failure, was: "
                        + status.getText());
    }
}
