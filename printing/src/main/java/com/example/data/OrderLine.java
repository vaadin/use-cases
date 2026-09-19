package com.example.data;

import java.math.BigDecimal;

/**
 * One line of an {@link Order}.
 *
 * @param sku
 *            the article number
 * @param description
 *            what the article is
 * @param quantity
 *            how many were ordered
 * @param unitPrice
 *            the price of one, in euro
 */
public record OrderLine(String sku, String description, int quantity,
        BigDecimal unitPrice) {

    /** The line total, i.e. {@code quantity × unitPrice}. */
    public BigDecimal total() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
