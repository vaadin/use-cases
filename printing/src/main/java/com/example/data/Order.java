package com.example.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A customer order — the document every use case in this module prints.
 *
 * @param id
 *            the order number, e.g. {@code ORD-1001}
 * @param customer
 *            the customer's name
 * @param address
 *            the delivery address, one line per element
 * @param ordered
 *            the order date
 * @param lines
 *            the ordered articles
 */
public record Order(String id, String customer, List<String> address,
        LocalDate ordered, List<OrderLine> lines) {

    /** The sum of all line totals. */
    public BigDecimal total() {
        return lines.stream().map(OrderLine::total).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    /** How many articles the order contains in total. */
    public int itemCount() {
        return lines.stream().mapToInt(OrderLine::quantity).sum();
    }
}
