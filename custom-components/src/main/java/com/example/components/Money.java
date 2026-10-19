package com.example.components;

import java.math.BigDecimal;
import java.util.Currency;

/** An amount in a currency. */
public record Money(BigDecimal amount, Currency currency) {

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount),
                Currency.getInstance(currency));
    }
}
