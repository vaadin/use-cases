package com.example.uc1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.IntegerField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Only the wiring: the rules themselves are covered by QuoteCalculatorTest.
 */
@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = QuoteView.class)
class QuoteViewTest extends SpringBrowserlessTest {

    @Test
    void showsTheCalculatorsQuoteAndFollowsTheInputs() {
        QuoteView view = navigate(QuoteView.class);
        runPendingSignalsTasks();
        assertEquals("UC1 — Logic without the UI",
                findInView(H1.class).single().getText());
        assertTrue(view.total().contains("10% off"), view.total());
        assertTrue(view.total().contains("total 268,92"), view.total());

        test(findInView(Checkbox.class).single()).click();
        test(findInView(IntegerField.class).single()).setValue(2);
        runPendingSignalsTasks();

        assertTrue(view.total().contains("5% off"), view.total());
        assertTrue(view.total().contains("shipping 7,90"), view.total());
    }
}
