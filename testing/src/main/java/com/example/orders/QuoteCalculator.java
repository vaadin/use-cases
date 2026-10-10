package com.example.orders;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/**
 * The pricing rules of the order desk, kept out of the UI so they can be tested
 * on their own (UC1):
 * <ul>
 * <li>10% off from 10 items, 15% off from 50 items;</li>
 * <li>another 5% off for business customers;</li>
 * <li>free shipping from 100 €, otherwise 7.90 €.</li>
 * </ul>
 */
@Component
public class QuoteCalculator {

    static final BigDecimal SHIPPING = new BigDecimal("7.90");
    static final BigDecimal FREE_SHIPPING_FROM = new BigDecimal("100.00");

    /** A price breakdown. */
    public record Quote(BigDecimal subtotal, int discountPercent,
            BigDecimal discount, BigDecimal shipping, BigDecimal total) {
    }

    public Quote quote(Product product, int quantity, boolean business) {
        if (quantity < 1) {
            throw new IllegalArgumentException(
                    "Quantity must be at least 1, was " + quantity);
        }
        BigDecimal subtotal = product.price()
                .multiply(BigDecimal.valueOf(quantity));
        int percent = quantity >= 50 ? 15 : quantity >= 10 ? 10 : 0;
        if (business) {
            percent += 5;
        }
        BigDecimal discount = subtotal.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal discounted = subtotal.subtract(discount);
        BigDecimal shipping = discounted.compareTo(FREE_SHIPPING_FROM) >= 0
                ? BigDecimal.ZERO.setScale(2)
                : SHIPPING;
        return new Quote(subtotal.setScale(2), percent, discount, shipping,
                discounted.add(shipping).setScale(2));
    }
}
