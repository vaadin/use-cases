package com.example.uc2;

import java.math.BigDecimal;

import com.example.common.BuiltWith;
import com.example.common.UseCaseDescription;
import com.example.components.Money;
import com.example.components.MoneyField;
import com.example.views.MainLayout;
import com.example.views.Sample;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC2 — A field that works with Binder and signals.
 * <p>
 * A refund form with two {@link MoneyField}s. One is bound with a Binder, with
 * required and range rules whose messages show on the field like on any
 * built-in field. The other is bound to a signal with {@code bindValue}, and a
 * summary follows it. A checkbox makes both read-only.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Field with Binder and signals")
@UseCaseDescription("Making a custom field that Binder, validation and signals treat like any other")
@Menu(order = 2, title = "UC2 — Field with Binder and signals")
public class MoneyFieldView extends VerticalLayout {

    static final BigDecimal MAX_REFUND = new BigDecimal("1000");

    /** What the form edits. */
    public static class Refund {
        private @Nullable Money amount;

        public @Nullable Money getAmount() {
            return amount;
        }

        public void setAmount(@Nullable Money amount) {
            this.amount = amount;
        }
    }

    private final MoneyField refund = new MoneyField("Refund");
    private final MoneyField fee = new MoneyField("Handling fee");
    private final ValueSignal<@Nullable Money> feeValue = new ValueSignal<>(
            Money.of("4.90", "EUR"));
    private final Binder<Refund> binder = new Binder<>();
    private final Span summary = new Span();
    private final Span result = new Span();

    public MoneyFieldView() {
        add(new H1("UC2 — Field with Binder and signals"));
        add(new Paragraph("The refund is bound with a Binder and checked; "
                + "the fee is bound to a signal that the summary follows."));

        refund.setRequiredIndicatorVisible(true);
        refund.setHelperText("At most 1 000 per refund");
        binder.forField(refund).asRequired("Enter an amount")
                .withValidator(money -> money.amount().signum() > 0,
                        "Must be more than zero")
                .withValidator(
                        money -> money.amount().compareTo(MAX_REFUND) <= 0,
                        "At most 1 000 per refund")
                .bind(Refund::getAmount, Refund::setAmount);
        binder.setBean(new Refund());

        fee.bindValue(feeValue, feeValue::set);
        summary.addClassName("status");
        summary.bindText(feeValue.map(money -> money == null ? "No fee"
                : "Fee: " + money.amount() + " "
                        + money.currency().getCurrencyCode()));

        Checkbox readOnly = new Checkbox("Read-only");
        readOnly.addValueChangeListener(e -> {
            refund.setReadOnly(e.getValue());
            fee.setReadOnly(e.getValue());
        });
        Button issue = new Button("Issue refund", e -> {
            Money amount = binder.getBean().getAmount();
            result.setText(binder.validate().isOk() && amount != null
                    ? "Refund of " + amount.amount() + " "
                            + amount.currency().getCurrencyCode() + " issued"
                    : "");
        });
        issue.addThemeVariants(ButtonVariant.PRIMARY);
        result.addClassName("status");

        add(new Sample("Refund", refund, fee, summary, readOnly, issue,
                result));
        add(new BuiltWith("MoneyField", "Custom field · CustomField<Money>")
                .point("CustomField brings a label, helper text, error message and required indicator, like any built-in field.")
                .point("generateModelValue builds a Money from the two inner fields; setPresentationValue splits it back.")
                .point("Being a HasValue, it is bound by Binder in one line and tied to a signal with bindValue.")
                .java("components/MoneyField").java("components/Money"));
    }

    // Package-private test seams.
    MoneyField refund() {
        return refund;
    }

    MoneyField fee() {
        return fee;
    }

    String summary() {
        return summary.getText();
    }

    String result() {
        return result.getText();
    }
}
