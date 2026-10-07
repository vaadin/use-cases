package com.example.uc7;

import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.GeolocationSimulator;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(classes = FormFieldView.class)
class FormFieldViewTest extends BrowserlessTest {

    @Test
    void pinningLocationWithGoodAccuracy_enablesSubmit_andFilesReport() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        geolocation.setLocation(51.5074, -0.1278, 20.0);
        navigate(FormFieldView.class);

        Button submit = find(Button.class).withText("Report pothole").single();
        assertFalse(submit.isEnabled(),
                "Submit should be disabled before description and location are set");

        TextField description = find(TextField.class).withCaption("Description")
                .single();
        test(description).setValue("Cracked pavement near bus stop");

        Button pin = find(Button.class).withText("Pin my location").single();
        test(pin).click();

        assertTrue(submit.isEnabled(),
                "Submit should be enabled after both description and good-accuracy location are set");

        Span pinLabel = find(Span.class).withTextContaining("Pinned at")
                .single();
        assertTrue(pinLabel.getText().contains("51.50740"),
                "Pin label should show latitude, was: " + pinLabel.getText());

        test(submit).click();

        Span resetLabel = find(Span.class).withText("No location pinned yet")
                .single();
        assertTrue(resetLabel.isVisible(),
                "Pin label should reset to default after submission");

        assertFalse(submit.isEnabled(),
                "Submit should be disabled again after successful submission");
    }

    @Test
    void pinningLocationWithPoorAccuracy_keepsSubmitDisabled() {
        GeolocationSimulator geolocation = GeolocationSimulator.current();
        geolocation.grantPermission();
        geolocation.setLocation(51.5074, -0.1278, 100.0);
        navigate(FormFieldView.class);

        TextField description = find(TextField.class).withCaption("Description")
                .single();
        test(description).setValue("Pothole on main road");

        Button pin = find(Button.class).withText("Pin my location").single();
        test(pin).click();

        Button submit = find(Button.class).withText("Report pothole").single();
        assertFalse(submit.isEnabled(),
                "Submit should remain disabled when accuracy exceeds the threshold");

        Span pinLabel = find(Span.class).withText("No location pinned yet")
                .single();
        assertTrue(pinLabel.isVisible(),
                "Pin label should stay at default when accuracy is too poor");
    }
}
