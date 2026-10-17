package com.example.uc3;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.GeolocationSimulator;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(classes = AutoFetchView.class)
class AutoFetchViewTest extends BrowserlessTest {

    @Test
    void hint_followsPermissionChangesWhileTheViewIsOpen() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        navigate(AutoFetchView.class);
        assertTrue(find(Span.class)
                .withText("Location permission has not been asked for yet.")
                .exists());

        geolocation.denyPermission();

        assertTrue(find(Span.class)
                .withText("Location permission was denied for this site.")
                .exists());
    }

    @Test
    void reattachingTheView_fetchesTheLocationAgain() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        geolocation.setLocation(60.1699, 24.9384, 50.0);
        AutoFetchView view = navigate(AutoFetchView.class);

        Element parent = view.getElement().getParent();
        view.getElement().removeFromParent();
        geolocation.setLocation(59.4370, 24.7536, 50.0);
        parent.appendChild(view.getElement());

        Span localContent = find(Span.class).withTextContaining("Local content")
                .single();
        assertTrue(localContent.getText().contains("59.4370"),
                "Re-attaching should fetch the new location, was: "
                        + localContent.getText());
    }

    @Test
    void attachWithLocationKnown_autoFetchesPosition() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        geolocation.setLocation(60.1699, 24.9384, 50.0);

        navigate(AutoFetchView.class);

        Span localContent = find(Span.class).withTextContaining("Local content")
                .single();
        assertTrue(localContent.getText().contains("60.1699"),
                "Local content should show latitude, was: "
                        + localContent.getText());
    }

    @Test
    void attachWithDeniedPermission_doesNotAutoFetch_showsButton() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.denyPermission();

        navigate(AutoFetchView.class);

        assertTrue(geolocation.requests().isEmpty(),
                "No pending get() request should remain when permission is DENIED");

        Button locate = find(Button.class).withText("Use my location").single();
        assertTrue(locate.isVisible(),
                "The explicit locate button should be visible for DENIED");

        Span hint = find(Span.class)
                .withTextContaining("Location permission was denied").single();
        assertEquals("Location permission was denied for this site.",
                hint.getText());
    }
}
