package com.example.uc1;

import com.example.components.Address;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = AddressFormView.class)
class AddressFormViewTest extends SpringBrowserlessTest {

    @Test
    void emptyBillingAddressIsRejectedOnItsFields() {
        AddressFormView view = navigate(AddressFormView.class);
        assertEquals("UC1 — Composite of existing components",
                findInView(H1.class).single().getText());

        test(button("Save addresses")).click();

        assertEquals("Check the highlighted fields", view.result());
        // The billing form's street; the shipping one is filled in.
        TextField billingStreet = streets()[1];
        assertTrue(billingStreet.isInvalid());
        assertEquals("Street is required", billingStreet.getErrorMessage());
    }

    @Test
    void sameAsShippingCopiesAndLocksTheBillingForm() {
        AddressFormView view = navigate(AddressFormView.class);

        test(view.sameAsShipping()).click();

        assertEquals(view.shipping().getAddress(), view.billing().getAddress());
        assertTrue(streets()[1].isReadOnly());
        test(button("Save addresses")).click();
        assertEquals(
                "Ship to Ruoholahdenkatu 21, 00180 Helsinki, Finland · "
                        + "bill to Ruoholahdenkatu 21, 00180 Helsinki, Finland",
                view.result());
    }

    @Test
    void validationAndReadingGoThroughTheFormsApi() {
        AddressFormView view = navigate(AddressFormView.class);
        TextField street = streets()[1];
        TextField postal = findInView(TextField.class).all().stream()
                .filter(f -> "Postal code".equals(f.getLabel())).toList()
                .get(1);
        @SuppressWarnings("unchecked")
        ComboBox<String> country = findInView(ComboBox.class).all().get(1);

        test(street).setValue("Hauptstraße 5");
        test(postal).setValue("not!");
        test(findInView(TextField.class).all().stream()
                .filter(f -> "City".equals(f.getLabel())).toList().get(1))
                .setValue("Berlin");
        test(country).selectItem("Germany");

        assertTrue(view.billing().validate().isEmpty());
        assertEquals("Not a postal code", postal.getErrorMessage());

        test(postal).setValue("10115");
        assertEquals(new Address("Hauptstraße 5", "10115", "Berlin", "Germany"),
                view.billing().validate().orElseThrow());
    }

    private TextField[] streets() {
        return findInView(TextField.class).all().stream()
                .filter(f -> "Street".equals(f.getLabel()))
                .toArray(TextField[]::new);
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }
}
