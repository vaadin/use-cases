package com.example.uc5;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(packages = "com.example")
class CustomerAddressViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersAddressFieldsAndUsesTheDraft() {
        navigate(CustomerDetailsView.class);
        test(findInView(TextField.class).single()).setValue("Harbor Café");

        navigate(CustomerAddressView.class);
        assertEquals("Address", findInView(H2.class).single().getText());
        assertEquals(2, findInView(TextField.class).all().size());
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getText().contains("Harbor Café")));
    }
}
