package com.example.uc5;

import com.example.components.SignaturePad;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drawing only exists in the browser (see SignaturePadPlaywrightIT); here the
 * pad's value and events are played the way the element would send them.
 */
@SpringBootTest
@ViewPackages(classes = SignatureView.class)
class SignatureViewTest extends SpringBrowserlessTest {

    static final String SIGNATURE = "data:image/png;base64,iVBORw0KGgo=";

    @Test
    void startingToSignIsAServerEvent() {
        SignatureView view = navigate(SignatureView.class);

        fireDomEvent(view.pad(), "signature-started");

        assertEquals("Signing…", view.status());
    }

    @Test
    void signatureArrivesAsTheValueAndEnablesConfirm() {
        SignatureView view = navigate(SignatureView.class);
        assertFalse(view.confirm().isEnabled());

        view.pad().setValue(SIGNATURE);

        assertTrue(view.pad().isSigned());
        assertEquals("Signed", view.status());
        assertEquals(SIGNATURE, view.preview().getSrc());
        assertTrue(view.confirm().isEnabled());

        test(view.confirm()).click();
        assertEquals("Delivery confirmed", view.status());
        assertEquals(Boolean.TRUE,
                view.pad().getElement().getProperty("disabled", false));
    }

    @Test
    void clearEmptiesThePad() {
        SignatureView view = navigate(SignatureView.class);
        view.pad().setValue(SIGNATURE);

        test(findInView(Button.class).all().stream()
                .filter(b -> "Clear".equals(b.getText())).findFirst()
                .orElseThrow()).click();

        assertFalse(view.pad().isSigned());
        assertEquals("Not signed", view.status());
        assertFalse(view.confirm().isEnabled());
    }

    private static void fireDomEvent(SignaturePad pad, String event) {
        new ComponentTester<SignaturePad>(pad) {
            void fire() {
                fireDomEvent(event);
            }
        }.fire();
    }
}
