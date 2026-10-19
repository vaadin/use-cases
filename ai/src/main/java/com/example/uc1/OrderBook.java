package com.example.uc1;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.example.uc1.Order.Status;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * The orders the assistant answers questions about. Read-only and the same for
 * every user. Dates are relative to the day the app started, so "late" and
 * "this month" questions always have answers.
 */
@Component
public class OrderBook {

    private final LocalDate today = LocalDate.now();

    private final List<Order> orders = List.of(
            order("SO-1001", "Northwind Traders", "Standing desk", 12,
                    "7188.00", Status.DELIVERED, 41, 20),
            order("SO-1002", "Contoso Ltd", "Office chair", 30, "8970.00",
                    Status.SHIPPED, 9, -3),
            order("SO-1003", "Fabrikam Inc", "Monitor arm", 25, "2225.00",
                    Status.OPEN, 16, 4),
            order("SO-1004", "Northwind Traders", "Office chair", 8, "2392.00",
                    Status.OPEN, 6, -10),
            order("SO-1005", "Adventure Works", "Standing desk", 4, "2396.00",
                    Status.CANCELLED, 22, 8),
            order("SO-1006", "Contoso Ltd", "Desk lamp", 60, "2940.00",
                    Status.DELIVERED, 35, 14),
            order("SO-1007", "Tailspin Toys", "Whiteboard", 6, "1494.00",
                    Status.OPEN, 3, -12),
            order("SO-1008", "Fabrikam Inc", "Standing desk", 20, "11980.00",
                    Status.SHIPPED, 12, -2),
            order("SO-1009", "Adventure Works", "Monitor arm", 15, "1335.00",
                    Status.OPEN, 2, -20),
            order("SO-1010", "Tailspin Toys", "Office chair", 10, "2990.00",
                    Status.DELIVERED, 28, 7),
            order("SO-1011", "Northwind Traders", "Desk lamp", 40, "1960.00",
                    Status.OPEN, 1, -25),
            order("SO-1012", "Contoso Ltd", "Standing desk", 16, "9584.00",
                    Status.OPEN, 19, 2));

    public List<Order> all() {
        return orders;
    }

    public LocalDate today() {
        return today;
    }

    /**
     * Returns the orders matching all given filters; a {@code null} filter
     * matches every order. The customer matches on any part of the name,
     * ignoring case.
     */
    public List<Order> find(@Nullable String customer, @Nullable Status status,
            boolean lateOnly) {
        return orders.stream()
                .filter(o -> customer == null || customer.isBlank()
                        || o.customer().toLowerCase(Locale.ROOT)
                                .contains(customer.toLowerCase(Locale.ROOT)))
                .filter(o -> status == null || o.status() == status)
                .filter(o -> !lateOnly || o.isLate(today)).toList();
    }

    public Optional<Order> get(String id) {
        return orders.stream().filter(o -> o.id().equalsIgnoreCase(id.trim()))
                .findFirst();
    }

    /**
     * @param daysAgo
     *            how many days before today the order was placed
     * @param dueInDays
     *            how many days after today the order is due; negative for a due
     *            date in the past
     */
    private Order order(String id, String customer, String product,
            int quantity, String amount, Status status, int daysAgo,
            int dueInDays) {
        return new Order(id, customer, product, quantity,
                new BigDecimal(amount), status, today.minusDays(daysAgo),
                today.plusDays(dueInDays));
    }
}
