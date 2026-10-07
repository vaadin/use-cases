package com.example.uc4;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.geolocation.GeolocationAvailability;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

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
}
