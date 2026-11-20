package com.example.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * An order of the made-up shop the filters work on.
 *
 * @param number
 *            the order number
 * @param date
 *            the day the order was placed
 * @param customer
 *            who ordered
 * @param total
 *            the order total in euros
 */
public record Order(String number, LocalDate date, String customer,
        BigDecimal total) {

    private static final String[] CUSTOMERS = { "Nordwind Logistics",
            "Lumen Dental", "Casa Verde", "Harbour Books", "Polar Bikes",
            "Studio Nine" };

    /**
     * Orders placed over the 120 days up to and including today, about one
     * every three days. The same today gives the same orders.
     *
     * @param today
     *            the last day with orders
     * @return the orders, newest first
     */
    public static List<Order> sample(LocalDate today) {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            LocalDate date = today.minusDays(i * 3L);
            BigDecimal total = BigDecimal.valueOf(40 + (i * 37 % 260))
                    .setScale(2);
            orders.add(new Order(String.format("A-%05d", 21000 - i), date,
                    CUSTOMERS[i % CUSTOMERS.length], total));
        }
        return List.copyOf(orders);
    }
}
