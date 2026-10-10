package com.example.uc7;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.GeolocationSimulator;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.shared.JsonConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = BrowserApisView.class)
class BrowserApisViewTest extends SpringBrowserlessTest {

    @Test
    void geolocationHasAProperSimulator() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        geolocation.setLocation(60.1699, 24.9384, 10.0);
        BrowserApisView view = navigate(BrowserApisView.class);

        test(findInView(Button.class).single()).click();

        assertEquals("Courier at 60.1699, 24.9384", view.position());
    }

    @Test
    void deniedPermissionIsSimulatedToo() {
        GeolocationSimulator.current().denyPermission();
        BrowserApisView view = navigate(BrowserApisView.class);

        test(findInView(Button.class).single()).click();

        assertEquals("Location unavailable: PERMISSION_DENIED",
                view.position());
    }

    @Test
    void localeCanBeSetButTheTimeZoneCannot() {
        BrowserApisView view = navigate(BrowserApisView.class);
        runPendingSignalsTasks();
        // A browserless UI reports no time zone, which reads as UTC; a test
        // has no way to pretend the browser is in Helsinki.
        assertEquals("Delivery Mar 5, 2026, 2:00 PM (Z)",
                view.window().replace(' ', ' '));

        UI.getCurrent().setLocale(Locale.GERMANY);
        runPendingSignalsTasks();

        assertEquals("Delivery 05.03.2026, 14:00 (Z)", view.window());
    }

    @Test
    void pageVisibilityIsFakedWithTheBrowsersEvent() {
        BrowserApisView view = navigate(BrowserApisView.class);
        runPendingSignalsTasks();
        assertEquals("Live tracking starting", view.tracking());

        // Gap: no simulator; fire the DOM event the browser would send.
        firePageVisibility("HIDDEN");
        runPendingSignalsTasks();
        assertEquals("Live tracking paused (tab hidden)", view.tracking());

        firePageVisibility("VISIBLE");
        runPendingSignalsTasks();
        assertEquals("Live tracking on", view.tracking());
    }

    private static void firePageVisibility(String state) {
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put("event.detail", state);
        // The listener is debounced, so the event must also say it is the
        // trailing call, as the browser's would.
        data.put(JsonConstants.EVENT_DATA_PHASE,
                DebouncePhase.TRAILING.getIdentifier());
        new ComponentTester<UI>(UI.getCurrent()) {
            void fire() {
                fireDomEvent("vaadin-page-visibility-change", data);
            }
        }.fire();
    }
}
