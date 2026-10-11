package com.example.uc1;

import com.example.common.UseCaseDescription;
import com.example.components.Address;
import com.example.components.AddressForm;
import com.example.views.MainLayout;
import com.example.views.Sample;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Composite of existing components.
 * <p>
 * The same {@link AddressForm} twice: a shipping and a billing address. The
 * view only uses the form's own API: set an address, read it, validate it, make
 * it read-only. "Same as shipping" copies the shipping address into the billing
 * form and locks it, without the view knowing which fields the form has.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Composite of existing components")
@UseCaseDescription("Building a reusable form from existing components with Composite")
@Menu(order = 1, title = "UC1 — Composite of existing components")
public class AddressFormView extends VerticalLayout {

    private final AddressForm shipping = new AddressForm();
    private final AddressForm billing = new AddressForm();
    private final Checkbox sameAsShipping = new Checkbox("Same as shipping");
    private final Span result = new Span();

    public AddressFormView() {
        add(new H1("UC1 — Composite of existing components"));
        add(new Paragraph("Two addresses, one component. Try saving with "
                + "empty fields, then tick \"Same as shipping\"."));

        shipping.setAddress(new Address("Ruoholahdenkatu 21", "00180",
                "Helsinki", "Finland"));
        sameAsShipping.addValueChangeListener(e -> {
            if (e.getValue()) {
                billing.setAddress(shipping.getAddress());
            }
            billing.setReadOnly(e.getValue());
        });

        Button save = new Button("Save addresses", e -> {
            if (sameAsShipping.getValue()) {
                billing.setAddress(shipping.getAddress());
            }
            var ship = shipping.validate();
            var bill = billing.validate();
            result.setText(ship.isPresent() && bill.isPresent()
                    ? "Ship to " + ship.get().singleLine() + " · bill to "
                            + bill.get().singleLine()
                    : "Check the highlighted fields");
        });
        save.addThemeVariants(ButtonVariant.PRIMARY);
        result.addClassName("status");

        add(new Sample("Order addresses", new H3("Shipping"), shipping,
                new H3("Billing"), sameAsShipping, billing, save, result));
    }

    // Package-private test seams.
    AddressForm shipping() {
        return shipping;
    }

    AddressForm billing() {
        return billing;
    }

    Checkbox sameAsShipping() {
        return sameAsShipping;
    }

    String result() {
        return result.getText();
    }
}
