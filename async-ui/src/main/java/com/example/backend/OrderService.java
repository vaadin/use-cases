package com.example.backend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

/**
 * A million orders that are never stored: each row is derived from its id, so
 * the "table" costs no memory and any page of it can be produced on demand —
 * the shape of a large database table behind an index on {@code id}.
 * <p>
 * Like such a table, it answers a page by id in constant time, but filtering
 * means a full scan, and so does an exact count of the matches.
 */
@Service
public class OrderService {

    public static final int ORDER_COUNT = 1_000_000;

    private static final List<String> CUSTOMERS = List.of("Aurora Bakery",
            "Brightside Dental", "Cobalt Logistics", "Delta Florists",
            "Evergreen Hostel", "Foxglove Studio", "Granite Works",
            "Harbor Café", "Ironleaf Gym", "Juniper Books", "Kestrel Air",
            "Lumen Optics", "Marigold Spa", "Nimbus Cloudware",
            "Oakridge School", "Pinecone Toys");

    private static final List<String> PRODUCTS = List.of("Espresso beans 1 kg",
            "Paper cups ×500", "Oat milk 12-pack", "Cleaning kit",
            "Receipt rolls ×50", "Milk jug", "Grinder burrs", "Descaler",
            "Barista apron", "Tamper");

    private static final LocalDate FIRST_DAY = LocalDate.of(2020, 1, 1);

    /** The order with the given id, derived from the id alone. */
    public Optional<Order> find(long id) {
        if (id < 1 || id > ORDER_COUNT) {
            return Optional.empty();
        }
        SplittableRandom random = new SplittableRandom(id);
        int quantity = 1 + random.nextInt(20);
        BigDecimal unitPrice = BigDecimal.valueOf(199 + random.nextInt(4800),
                2);
        return Optional.of(
                new Order(id, CUSTOMERS.get(random.nextInt(CUSTOMERS.size())),
                        PRODUCTS.get(random.nextInt(PRODUCTS.size())), quantity,
                        unitPrice.multiply(BigDecimal.valueOf(quantity))
                                .setScale(2, RoundingMode.HALF_UP),
                        FIRST_DAY.plusDays(id * 2190 / ORDER_COUNT)));
    }

    /**
     * One page of the orders whose customer contains {@code filter}
     * (case-insensitively), ordered by id.
     */
    public Stream<Order> fetch(String filter, boolean descending, int offset,
            int limit) {
        return matching(filter, descending).skip(offset).limit(limit);
    }

    /**
     * The exact number of orders matching {@code filter}: with a filter, a scan
     * of the whole table.
     */
    public int count(String filter) {
        return filter.isBlank() ? ORDER_COUNT
                : (int) matching(filter, false).count();
    }

    private Stream<Order> matching(String filter, boolean descending) {
        LongStream ids = LongStream.rangeClosed(1, ORDER_COUNT);
        if (descending) {
            ids = ids.map(id -> ORDER_COUNT + 1 - id);
        }
        Stream<Order> orders = ids.mapToObj(id -> find(id).orElseThrow());
        if (filter.isBlank()) {
            return orders;
        }
        String needle = filter.toLowerCase(Locale.ROOT);
        return orders.filter(order -> order.customer().toLowerCase(Locale.ROOT)
                .contains(needle));
    }
}
