package com.example.components;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;

/**
 * An amount and its currency as one field (UC2).
 * <p>
 * Extending {@link CustomField} makes it a {@code HasValue<Money>} with a
 * label, helper text, an error message and a required indicator like any other
 * field, so a Binder binds it with one line and {@code bindValue} ties it to a
 * signal. The two inner fields are an implementation detail: a value change is
 * one event for the whole field, and {@link #setReadOnly} and
 * {@link #setEnabled} reach both.
 */
public class MoneyField extends CustomField<@Nullable Money> {

    static final List<Currency> CURRENCIES = List.of(
            Currency.getInstance("EUR"), Currency.getInstance("USD"),
            Currency.getInstance("SEK"));

    private final BigDecimalField amount = new BigDecimalField();
    private final Select<Currency> currency = new Select<>();

    public MoneyField(String label) {
        setLabel(label);
        amount.setAriaLabel(label + " amount");
        amount.setWidth("9em");
        currency.setAriaLabel(label + " currency");
        currency.setItems(CURRENCIES);
        currency.setItemLabelGenerator(Currency::getCurrencyCode);
        currency.setValue(CURRENCIES.getFirst());
        currency.setWidth("6em");
        add(new HorizontalLayout(amount, currency));
        // CustomField takes its value from the browser's "change" event of
        // the whole field, so a change made to an inner field on the server
        // (or in a test) would never reach it. Updating on the inner fields'
        // own value changes covers both (API-GAPS.md #1,
        // vaadin/flow-components#10426).
        amount.addValueChangeListener(e -> updateValue());
        currency.addValueChangeListener(e -> updateValue());
    }

    @Override
    protected @Nullable Money generateModelValue() {
        BigDecimal value = amount.getValue();
        return value == null ? null
                : new Money(value, Objects.requireNonNullElse(
                        currency.getValue(), CURRENCIES.getFirst()));
    }

    @Override
    protected void setPresentationValue(@Nullable Money money) {
        amount.setValue(money == null ? null : money.amount());
        currency.setValue(
                money == null ? CURRENCIES.getFirst() : money.currency());
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        amount.setReadOnly(readOnly);
        currency.setReadOnly(readOnly);
    }
}
