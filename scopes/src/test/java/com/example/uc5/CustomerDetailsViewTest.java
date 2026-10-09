package com.example.uc5;

import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(packages = "com.example")
class CustomerDetailsViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersInsideTheEditorLayout() {
        navigate(CustomerDetailsView.class);

        assertTrue($(H1.class).all().stream().anyMatch(
                h -> "UC5 — Customer draft across pages".equals(h.getText())));
        assertEquals("Contact details",
                findInView(H2.class).single().getText());
        findInView(TextField.class).single();
        findInView(EmailField.class).single();
    }

    @Test
    void draftIsKeptInsideTheEditorAndDiscardedOnLeaving() {
        navigate(CustomerDetailsView.class);
        String draft = badge();
        test(findInView(TextField.class).single()).setValue("Nordic Bikes");

        // Same draft on the second page of the editor.
        navigate(CustomerAddressView.class);
        assertEquals(draft, badge());
        test(findInView(TextField.class).withCaption("City").single())
                .setValue("Oslo");

        navigate(CustomerDetailsView.class);
        assertEquals(draft, badge());
        assertEquals("Nordic Bikes",
                findInView(TextField.class).single().getValue());

        // Leaving the editor discards the draft.
        navigate(HomeView.class);
        navigate(CustomerDetailsView.class);
        assertNotEquals(draft, badge());
        assertEquals("", findInView(TextField.class).single().getValue());
    }

    private String badge() {
        return findInView(Span.class).withClassName("scope-badge").single()
                .getText();
    }
}
