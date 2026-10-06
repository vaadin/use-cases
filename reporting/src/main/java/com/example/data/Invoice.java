package com.example.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * An invoice — the document this module generates, previews and delivers.
 *
 * @param number
 *            the invoice number, e.g. {@code 2026-0007}
 * @param customer
 *            the customer's name
 * @param address
 *            the invoicing address, one line per element
 * @param issued
 *            the invoice date
 * @param dueDays
 *            the payment term, in days from {@code issued}
 * @param lines
 *            the billed lines
 */
public record Invoice(String number, String customer, List<String> address,
        LocalDate issued, int dueDays, List<InvoiceLine> lines) {

    /** The date the invoice is due. */
    public LocalDate due() {
        return issued.plusDays(dueDays);
    }

    /** The sum of all lines, excluding VAT. */
    public BigDecimal net() {
        return sum(InvoiceLine::net);
    }

    /** The VAT charged across all lines. */
    public BigDecimal vat() {
        return sum(InvoiceLine::vat);
    }

    /** What the customer owes. */
    public BigDecimal gross() {
        return net().add(vat());
    }

    private BigDecimal sum(Function<InvoiceLine, BigDecimal> part) {
        return lines.stream().map(part).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }
}
