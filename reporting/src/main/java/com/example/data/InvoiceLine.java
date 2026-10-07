package com.example.data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One billed line of an {@link Invoice}.
 *
 * @param description
 *            what is being billed
 * @param quantity
 *            how many units
 * @param unitPrice
 *            the price of one unit, excluding VAT
 * @param vatPercent
 *            the VAT rate applied to this line
 */
public record InvoiceLine(String description, int quantity,
        BigDecimal unitPrice, int vatPercent) {

    /** The line total, excluding VAT. */
    public BigDecimal net() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2,
                RoundingMode.HALF_UP);
    }

    /** The VAT charged on this line. */
    public BigDecimal vat() {
        return net().multiply(BigDecimal.valueOf(vatPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** The line total, including VAT. */
    public BigDecimal gross() {
        return net().add(vat());
    }
}
