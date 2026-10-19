package com.example.orders;

import java.math.BigDecimal;

import com.example.orders.QuoteCalculator.Quote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Plain JUnit: no Spring context, no Vaadin, runs in milliseconds. */
class QuoteCalculatorTest {

    private final QuoteCalculator calculator = new QuoteCalculator();

    @ParameterizedTest(name = "{1} × {0}, business={2} → {3}% off")
    @CsvSource({ "ESPRESSO_BEANS, 1, false, 0", "ESPRESSO_BEANS, 9, false, 0",
            "ESPRESSO_BEANS, 10, false, 10", "ESPRESSO_BEANS, 49, false, 10",
            "ESPRESSO_BEANS, 50, false, 15", "ESPRESSO_BEANS, 1, true, 5",
            "ESPRESSO_BEANS, 50, true, 20" })
    void volumeAndBusinessDiscounts(Product product, int quantity,
            boolean business, int percent) {
        assertEquals(percent, calculator.quote(product, quantity, business)
                .discountPercent());
    }

    @Test
    void smallOrderPaysShipping() {
        Quote quote = calculator.quote(Product.ESPRESSO_BEANS, 2, false);

        assertEquals(new BigDecimal("49.80"), quote.subtotal());
        assertEquals(new BigDecimal("7.90"), quote.shipping());
        assertEquals(new BigDecimal("57.70"), quote.total());
    }

    @Test
    void shippingIsFreeFromOneHundredAfterDiscount() {
        // 12 × 24.90 = 298.80, 10% off = 268.92
        Quote quote = calculator.quote(Product.ESPRESSO_BEANS, 12, false);

        assertEquals(new BigDecimal("29.88"), quote.discount());
        assertEquals(new BigDecimal("0.00"), quote.shipping());
        assertEquals(new BigDecimal("268.92"), quote.total());
    }

    @Test
    void discountCanDropAnOrderBelowFreeShipping() {
        // 1 × 189.00 is above the limit; never below because of discounts
        // alone here, but 4 × 24.90 = 99.60 is just under it.
        assertEquals(new BigDecimal("7.90"),
                calculator.quote(Product.ESPRESSO_BEANS, 4, false).shipping());
        assertEquals(new BigDecimal("0.00"),
                calculator.quote(Product.GRINDER, 1, false).shipping());
    }

    @Test
    void quantityMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.quote(Product.GRINDER, 0, false));
    }
}
