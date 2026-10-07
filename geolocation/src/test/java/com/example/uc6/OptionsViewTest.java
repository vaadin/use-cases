package com.example.uc6;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.GeolocationOptions;
import com.vaadin.flow.component.geolocation.GeolocationRequest;
import com.vaadin.flow.component.geolocation.GeolocationSimulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(classes = OptionsView.class)
class OptionsViewTest extends BrowserlessTest {

    @Test
    void runProfileC_passesHighAccuracyTimeoutAndZeroMaxAge() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        navigate(OptionsView.class);

        List<Button> runs = find(Button.class).withText("Run").all();
        assertEquals(4, runs.size(), "Expected 4 Run buttons, one per profile");

        Button profileC = runs.get(2);
        test(profileC).click();

        GeolocationRequest req = geolocation.lastRequest()
                .orElseThrow(() -> new AssertionError(
                        "No pending request after clicking profile C"));

        GeolocationOptions opts = req.options();
        assertNotNull(opts, "Profile C should pass non-null options");
        assertEquals(Boolean.TRUE, opts.enableHighAccuracy(),
                "Profile C should request high accuracy");
        assertEquals(10_000, opts.timeout(),
                "Profile C timeout should be 10 000 ms");
        assertEquals(0, opts.maximumAge(),
                "Profile C maximumAge should be 0 (must be fresh)");
        assertTrue(req.isPending(), "Request should still be pending");
    }
}
