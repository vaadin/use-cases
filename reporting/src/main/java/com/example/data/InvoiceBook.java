package com.example.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * Remembers which invoices have been delivered, and when.
 * <p>
 * Application-scoped on purpose: an invoice that one user has sent is sent for
 * everyone, so the state has to be shared between sessions rather than kept in
 * a view.
 */
@Service
public class InvoiceBook {

    /** One delivery, as the book records it. */
    public record Delivery(String invoiceNumber, long bytes) {
    }

    private final Map<String, Delivery> deliveries = Collections
            .synchronizedMap(new LinkedHashMap<>());

    /**
     * Records that an invoice reached the customer.
     *
     * @param invoiceNumber
     *            the invoice number
     * @param bytes
     *            how many bytes were transferred
     */
    public void markSent(String invoiceNumber, long bytes) {
        deliveries.put(invoiceNumber, new Delivery(invoiceNumber, bytes));
    }

    /**
     * @param invoiceNumber
     *            the invoice number
     * @return the delivery, if the invoice has been sent
     */
    public Optional<Delivery> delivery(String invoiceNumber) {
        return Optional.ofNullable(deliveries.get(invoiceNumber));
    }

    /** Forgets every delivery — used by the demo's Reset button. */
    public void clear() {
        deliveries.clear();
    }
}
