package com.example.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The money arithmetic, pinned to values worked out by hand. Everything else in
 * this module only moves these numbers around.
 */
class InvoiceTest {

    @Test
    void aLineRoundsVatToTheCentAndHalfUp() {
        // 3 × 12.345 = 37.035, which rounds up to 37.04; 24 % of that is
        // 8.8896, which rounds up to 8.89.
        InvoiceLine line = new InvoiceLine("Espresso blend, 1 kg", 3,
                new BigDecimal("12.345"), 24);

        assertEquals(new BigDecimal("37.04"), line.net());
        assertEquals(new BigDecimal("8.89"), line.vat());
        assertEquals(new BigDecimal("45.93"), line.gross());
    }

    @Test
    void aLineWithoutVatIsItsOwnGross() {
        InvoiceLine line = new InvoiceLine("Delivery, city area", 2,
                new BigDecimal("7.50"), 0);

        assertEquals(new BigDecimal("15.00"), line.net());
        assertEquals(new BigDecimal("0.00"), line.vat());
        assertEquals(new BigDecimal("15.00"), line.gross());
    }

    @Test
    void anInvoiceSumsItsLinesAtDifferentVatRates() {
        Invoice invoice = new Invoice("2026-9999", "Northwind Traders",
                List.of("Northwind Traders"), LocalDate.of(2026, 2, 2), 14,
                List.of(new InvoiceLine("Filter blend, 1 kg", 4,
                        new BigDecimal("18.00"), 24),
                        new InvoiceLine("Barista training, hour", 2,
                                new BigDecimal("55.00"), 14)));

        // 72.00 + 110.00 net, 17.28 + 15.40 VAT.
        assertEquals(new BigDecimal("182.00"), invoice.net());
        assertEquals(new BigDecimal("32.68"), invoice.vat());
        assertEquals(new BigDecimal("214.68"), invoice.gross(),
                "The total due is the sum of the lines, not of rounded "
                        + "subtotals");
    }

    @Test
    void anInvoiceIsDueTheAgreedNumberOfDaysAfterIssue() {
        Invoice invoice = Invoices.sampleInvoice();

        assertEquals(invoice.issued().plusDays(14), invoice.due());
    }
}
