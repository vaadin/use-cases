package com.example.components;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

/**
 * An address form built from existing components (UC1).
 * <p>
 * A {@link Composite} hides the layout it wraps: callers see
 * {@link #getAddress()}, {@link #setAddress(Address)},
 * {@link #setReadOnly(boolean)} and {@link #validate()}, not a
 * {@link FormLayout} they could add things to or rearrange. The fields and the
 * rules stay in one place, and every screen that asks for an address asks for
 * it the same way.
 */
public class AddressForm extends Composite<FormLayout> {

    static final List<String> COUNTRIES = List.of("Finland", "Germany",
            "Sweden", "United States");

    private final TextField street = new TextField("Street");
    private final TextField postalCode = new TextField("Postal code");
    private final TextField city = new TextField("City");
    private final ComboBox<String> country = new ComboBox<>("Country",
            COUNTRIES);
    private final Binder<Address> binder = new Binder<>();

    public AddressForm() {
        binder.forField(street).asRequired("Street is required")
                .bind(Address::street, AddressForm::ignore);
        binder.forField(postalCode).asRequired("Postal code is required")
                .withValidator(code -> code.matches("[0-9A-Z -]{3,10}"),
                        "Not a postal code")
                .bind(Address::postalCode, AddressForm::ignore);
        binder.forField(city).asRequired("City is required").bind(Address::city,
                AddressForm::ignore);
        binder.forField(country).asRequired("Country is required")
                .bind(Address::country, AddressForm::ignore);
        setAddress(Address.EMPTY);
    }

    @Override
    protected FormLayout initContent() {
        FormLayout layout = new FormLayout(street, postalCode, city, country);
        layout.setColspan(street, 2);
        return layout;
    }

    public void setAddress(Address address) {
        binder.readBean(address);
    }

    /** The address as currently entered, valid or not. */
    public Address getAddress() {
        return new Address(street.getValue(), postalCode.getValue(),
                city.getValue(),
                country.getValue() == null ? "" : country.getValue());
    }

    /**
     * Checks every field and shows the errors on them.
     *
     * @return the address, if it is valid
     */
    public Optional<Address> validate() {
        return binder.validate().isOk() ? Optional.of(getAddress())
                : Optional.empty();
    }

    /**
     * The binder only validates and reads: an {@link Address} is immutable, and
     * {@link #getAddress()} builds a new one from the fields.
     */
    private static void ignore(Address address, String value) {
    }

    public void setReadOnly(boolean readOnly) {
        List.<HasValue<?, ?>> of(street, postalCode, city, country)
                .forEach(field -> field.setReadOnly(readOnly));
    }
}
