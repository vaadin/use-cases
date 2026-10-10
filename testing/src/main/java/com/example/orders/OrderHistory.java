package com.example.orders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

/**
 * Every order: the ones placed in the application, newest first, followed by
 * 100,000 generated past orders. Answers the paged, sorted and filtered queries
 * a lazy Grid sends (UC4).
 */
@Component
public class OrderHistory {

    public static final int PAST_ORDERS = 100_000;

    private static final List<String> CUSTOMERS = List.of("Kestrel Air",
            "Blue Finch", "Harbour Foods", "Northwind", "Aalto Cafe",
            "Lindqvist & Co", "Old Mill Bakery", "Café Sol");

    private final OrderStore store;
    private final List<Order> past;

    public OrderHistory(OrderStore store) {
        this.store = store;
        Product[] products = Product.values();
        past = IntStream.rangeClosed(1, PAST_ORDERS).mapToObj(n -> {
            Product product = products[n % products.length];
            int quantity = n % 40 + 1;
            return new Order(n, CUSTOMERS.get(n % CUSTOMERS.size()), product,
                    quantity, LocalDate.of(2025, 1, 1).plusDays(n % 365),
                    product.price().multiply(BigDecimal.valueOf(quantity)));
        }).toList();
    }

    /** How to sort: by number, customer or total, either direction. */
    public record Sort(String property, boolean ascending) {
    }

    public Stream<Order> fetch(String filter, List<Sort> sorts, int offset,
            int limit) {
        return matching(filter, sorts).skip(offset).limit(limit);
    }

    public int count(String filter) {
        return (int) matching(filter, List.of()).count();
    }

    private Stream<Order> matching(String filter, List<Sort> sorts) {
        List<Order> placed = new ArrayList<>(store.all());
        placed.sort(Comparator.comparingInt(Order::number).reversed());
        String term = filter.strip().toLowerCase(Locale.ROOT);
        Stream<Order> all = Stream.concat(placed.stream(), past.stream())
                .filter(order -> term.isEmpty() || order.customer()
                        .toLowerCase(Locale.ROOT).contains(term));
        Comparator<Order> comparator = null;
        for (Sort sort : sorts) {
            Comparator<Order> next = switch (sort.property()) {
            case "customer" -> Comparator.comparing(Order::customer);
            case "total" -> Comparator.comparing(Order::total);
            default -> Comparator.comparingInt(Order::number);
            };
            if (!sort.ascending()) {
                next = next.reversed();
            }
            comparator = comparator == null ? next
                    : comparator.thenComparing(next);
        }
        return comparator == null ? all : all.sorted(comparator);
    }
}
