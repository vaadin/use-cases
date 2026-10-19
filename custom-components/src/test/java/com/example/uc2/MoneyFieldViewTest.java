package com.example.uc2;

import java.math.BigDecimal;
import java.util.Currency;

import com.example.components.Money;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MoneyFieldView.class)
class MoneyFieldViewTest extends SpringBrowserlessTest {

    @Test
    void binderRulesShowOnTheCustomField() {
        MoneyFieldView view = navigate(MoneyFieldView.class);

        test(issue()).click();
        assertTrue(view.refund().isInvalid());
        assertEquals("Enter an amount", view.refund().getErrorMessage());

        test(refundAmount()).setValue(new BigDecimal("5000"));
        assertTrue(view.refund().isInvalid());
        assertEquals("At most 1 000 per refund",
                view.refund().getErrorMessage());
    }

    @Test
    void innerFieldsMakeOneValue() {
        MoneyFieldView view = navigate(MoneyFieldView.class);

        test(refundAmount()).setValue(new BigDecimal("24.50"));
        @SuppressWarnings("unchecked")
        Select<Currency> currency = findInView(Select.class).all().getFirst();
        test(currency).selectItem("SEK");
        test(issue()).click();

        assertEquals(Money.of("24.50", "SEK"), view.refund().getValue());
        assertEquals("Refund of 24.50 SEK issued", view.result());
    }

    @Test
    void signalBindingFollowsBothWays() {
        MoneyFieldView view = navigate(MoneyFieldView.class);
        runPendingSignalsTasks();
        assertEquals("Fee: 4.90 EUR", view.summary());
        assertEquals(Money.of("4.90", "EUR"), view.fee().getValue());

        BigDecimalField feeAmount = findInView(BigDecimalField.class).all()
                .get(1);
        test(feeAmount).setValue(new BigDecimal("6.00"));
        runPendingSignalsTasks();

        assertEquals("Fee: 6.00 EUR", view.summary());
    }

    @Test
    void readOnlyReachesTheInnerFields() {
        navigate(MoneyFieldView.class);

        test(findInView(Checkbox.class).single()).click();

        assertTrue(findInView(BigDecimalField.class).all().stream()
                .allMatch(BigDecimalField::isReadOnly));
    }

    private BigDecimalField refundAmount() {
        return findInView(BigDecimalField.class).all().getFirst();
    }

    private Button issue() {
        return findInView(Button.class).single();
    }
}
