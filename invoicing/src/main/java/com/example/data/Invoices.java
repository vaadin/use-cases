package com.example.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic sample invoices, so that a generated document and the test that
 * reads it back always agree on the numbers.
 */
public final class Invoices {

    /** The company issuing the invoices in this demo. */
    public static final List<String> SELLER = List.of("Kettle & Cup Oy",
            "Harbour Road 4", "20100 Turku, Finland", "VAT FI12345678");

    private static final LocalDate FIRST_ISSUE_DATE = LocalDate.of(2026, 2, 2);

    private static final List<String> SERVICES = List.of("Espresso blend, 1 kg",
            "Filter blend, 1 kg", "Decaf blend, 1 kg", "Barista training, hour",
            "Grinder service visit", "Cup rental, month",
            "Delivery, city area");

    private static final List<String> CUSTOMERS = List.of("Northwind Traders",
            "Blue Harbour Café", "Lakeside Hotel", "Pinecrest Bakery",
            "Harbourview Bistro", "Old Mill Roastery");

    private Invoices() {
    }

    /**
     * Builds {@code count} invoices, numbered {@code 2026-0001} upwards.
     *
     * @param count
     *            how many invoices to build
     * @return the invoices, always identical for the same {@code count}
     */
    public static List<Invoice> sample(int count) {
        Random random = new Random(20260202L);
        List<Invoice> invoices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String customer = CUSTOMERS.get(i % CUSTOMERS.size());
            invoices.add(new Invoice("2026-%04d".formatted(i + 1), customer,
                    List.of(customer, (10 + i) + " Market Street",
                            "20100 Turku", "Finland"),
                    FIRST_ISSUE_DATE.plusDays(i * 2L), 14,
                    lines(random, 2 + random.nextInt(4))));
        }
        return List.copyOf(invoices);
    }

    /**
     * The invoice the single-document use cases work with.
     *
     * @return the first sample invoice
     */
    public static Invoice sampleInvoice() {
        return sample(1).getFirst();
    }

    private static List<InvoiceLine> lines(Random random, int lineCount) {
        List<InvoiceLine> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            lines.add(
                    new InvoiceLine(
                            SERVICES.get(random.nextInt(SERVICES.size()))
                                    + " (" + (i + 1) + ")",
                            1 + random.nextInt(9),
                            BigDecimal.valueOf(12 + random.nextInt(60)).add(
                                    BigDecimal.valueOf(random.nextInt(100), 2)),
                            random.nextBoolean() ? 24 : 14));
        }
        return List.copyOf(lines);
    }
}
