package com.example.uc4;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.GeolocationAvailability;
import com.vaadin.flow.component.geolocation.GeolocationSimulator;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(classes = DenialView.class)
class DenialViewTest extends BrowserlessTest {

    @Test
    void availabilityCard_setsDeniedAvailability_showsPostcodeField() {
        navigate(DenialView.class);

        @SuppressWarnings("unchecked")
        Select<GeolocationAvailability> select = find(Select.class)
                .withCaption("Browser-reported availability").single();
        test(select).selectItem("DENIED");

        TextField postcode = find(TextField.class).withCaption("Postcode")
                .single();
        assertTrue(postcode.isVisible(),
                "Postcode field should be visible when availability is DENIED");

        Span hint = find(Span.class).withTextContaining("Location is blocked")
                .single();
        assertTrue(hint.isVisible(),
                "Hint span should be visible when availability is DENIED");
    }

    @Test
    void availabilityCard_unsupported_hidesTheLocateButton() {
        navigate(DenialView.class);

        @SuppressWarnings("unchecked")
        Select<GeolocationAvailability> select = find(Select.class)
                .withCaption("Browser-reported availability").single();
        test(select).selectItem("UNSUPPORTED");

        assertFalse(find(Button.class).withText("Find stores near me").exists(),
                "The locate button should be hidden when UNSUPPORTED");
    }

    @Test
    void requestOutcomeCard_unknownErrorCode_showsTheGenericMessage() {
        navigate(DenialView.class);

        @SuppressWarnings("unchecked")
        Select<Object> select = find(Select.class)
                .withCaption("What the request returns").single();
        test(select).selectItem("Error — unknown future code (99)");
        test(find(Button.class).withText("Run request").single()).click();

        assertTrue(find(Span.class).withText("Could not get your location.")
                .exists());
    }

    @Test
    void realBrowserCard_showsTheLiveAvailabilityAndTheDeniedError() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        navigate(DenialView.class);
        assertTrue(find(Span.class).withText("Current availability: PROMPT")
                .exists());

        geolocation.denyPermission();
        assertTrue(find(Span.class).withText("Current availability: DENIED")
                .exists());

        test(find(Button.class).withText("Use my location").single()).click();
        assertTrue(find(Span.class).withText("Location permission was denied.")
                .exists());
    }
}
