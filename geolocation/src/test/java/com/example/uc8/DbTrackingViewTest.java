package com.example.uc8;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.GeolocationErrorCode;
import com.vaadin.flow.component.geolocation.GeolocationSimulator;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DbTrackingView.class)
class DbTrackingViewTest extends SpringBrowserlessTest {

    @Test
    @SuppressWarnings("unchecked")
    void startTracking_userMoves_persistsPositionsAndGrowsGrid() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        navigate(DbTrackingView.class);

        Grid<TrackedPosition> grid = findInView(Grid.class).single();
        assertEquals(0, test(grid).size(),
                "Grid should be empty before tracking starts");

        Button start = find(Button.class).withText("Start tracking").single();
        test(start).click();

        geolocation.setLocation(60.1699, 24.9384, 15.0);
        Span status = find(Span.class).withText("Saved update #1 to DB")
                .single();
        assertEquals("Saved update #1 to DB", status.getText());
        assertEquals(1, test(grid).size(),
                "Grid should reflect one row from the database");

        geolocation.setLocation(60.1700, 24.9385, 12.0);
        status = find(Span.class).withText("Saved update #2 to DB").single();
        assertEquals("Saved update #2 to DB", status.getText());
        assertEquals(2, test(grid).size(),
                "Grid should reflect two rows from the database");
    }

    @Test
    @SuppressWarnings("unchecked")
    void clearHistory_afterUpdates_emptiesGrid() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        navigate(DbTrackingView.class);

        test(find(Button.class).withText("Start tracking").single()).click();
        geolocation.setLocation(60.1699, 24.9384, 15.0);
        geolocation.setLocation(60.1700, 24.9385, 12.0);

        Grid<TrackedPosition> grid = findInView(Grid.class).single();
        assertEquals(2, test(grid).size(),
                "Grid should have two rows before clearing");

        test(find(Button.class).withText("Clear history").single()).click();

        assertEquals(0, test(grid).size(),
                "Grid should be empty after clearing history");
    }

    @Test
    void stopTracking_afterUpdates_showsStoppedStatus() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        navigate(DbTrackingView.class);

        test(find(Button.class).withText("Start tracking").single()).click();
        geolocation.setLocation(60.1699, 24.9384, 15.0);

        test(find(Button.class).withText("Stop tracking").single()).click();

        Span status = find(Span.class).withTextContaining("Stopped after")
                .single();
        assertTrue(status.getText().contains("1"),
                "Stopped status should reflect one update, was: "
                        + status.getText());
        assertTrue(
                find(Button.class).withText("Resume tracking").single()
                        .isVisible(),
                "Start button should be re-labelled to Resume after stop");
    }

    @Test
    void positionUnavailableMidTrack_displaysErrorStatus() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        navigate(DbTrackingView.class);

        test(find(Button.class).withText("Start tracking").single()).click();

        geolocation.setUnavailable(GeolocationErrorCode.POSITION_UNAVAILABLE,
                "position unavailable");

        Span status = find(Span.class).withTextContaining("Could not determine")
                .single();
        assertTrue(status.getText().contains("location"),
                "Status should describe the location failure, was: "
                        + status.getText());
    }
}
