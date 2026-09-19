package com.example.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Deterministic sample orders, so that a printed document and the test that
 * asserts on it always see the same numbers.
 */
public final class Orders {

    private static final LocalDate FIRST_ORDER_DATE = LocalDate.of(2026, 1, 12);

    private static final List<String> ARTICLES = List.of("Ceramic mug",
            "Espresso cup", "Tea pot", "French press", "Milk jug", "Sugar bowl",
            "Cake plate", "Serving tray", "Water carafe", "Wine glass",
            "Napkin set", "Table cloth");

    private static final List<String> CUSTOMERS = List.of("Northwind Traders",
            "Blue Harbour Café", "Lakeside Hotel", "Pinecrest Bakery",
            "Harbourview Bistro", "Old Mill Roastery", "Sunset Diner",
            "The Copper Kettle");

    private Orders() {
    }

    /**
     * Builds {@code count} orders, numbered {@code ORD-1001} upwards.
     *
     * @param count
     *            how many orders to build
     * @return the orders, always identical for the same {@code count}
     */
    public static List<Order> sample(int count) {
        Random random = new Random(20260119L);
        List<Order> orders = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String id = "ORD-" + (1001 + i);
            String customer = CUSTOMERS.get(i % CUSTOMERS.size());
            orders.add(new Order(id, customer,
                    List.of(customer, (100 + i) + " Market Street",
                            "20100 Turku", "Finland"),
                    FIRST_ORDER_DATE.plusDays(i),
                    lines(random, 2 + random.nextInt(5))));
        }
        return List.copyOf(orders);
    }

    /**
     * The order the single-document use cases print.
     *
     * @return an order with a handful of lines
     */
    public static Order sampleOrder() {
        return sample(1).getFirst();
    }

    /**
     * An order long enough to need several sheets of paper.
     *
     * @param lineCount
     *            how many lines the order should have
     * @return the order
     */
    public static Order longOrder(int lineCount) {
        Order first = sampleOrder();
        return new Order("ORD-2001", first.customer(), first.address(),
                first.ordered(), lines(new Random(20260119L), lineCount));
    }

    /**
     * Looks an order up by its number.
     *
     * @param id
     *            the order number
     * @param count
     *            how many orders to look through
     * @return the order, if there is one with that number
     */
    public static Optional<Order> byId(String id, int count) {
        return sample(count).stream().filter(order -> order.id().equals(id))
                .findFirst();
    }

    private static List<OrderLine> lines(Random random, int lineCount) {
        List<OrderLine> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            String article = ARTICLES.get(random.nextInt(ARTICLES.size()));
            lines.add(new OrderLine(
                    "SKU-%04d".formatted(1000 + random.nextInt(9000)),
                    article + " (" + (i + 1) + ")", 1 + random.nextInt(12),
                    BigDecimal.valueOf(4 + random.nextInt(40))
                            .add(BigDecimal.valueOf(random.nextInt(100), 2))
                            .setScale(2, RoundingMode.HALF_UP)));
        }
        return List.copyOf(lines);
    }
}
