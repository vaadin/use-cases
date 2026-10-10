package com.example.orders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/** The placed orders, shared by every session; stands in for a database. */
@Component
public class OrderStore {

    private final AtomicInteger nextNumber = new AtomicInteger(200_001);
    private final List<Order> orders = new ArrayList<>();

    public synchronized Order place(String customer, Product product,
            int quantity, LocalDate delivery, BigDecimal total) {
        Order order = new Order(nextNumber.getAndIncrement(), customer, product,
                quantity, delivery, total);
        orders.add(order);
        return order;
    }

    public synchronized List<Order> all() {
        return List.copyOf(orders);
    }

    public synchronized boolean cancel(int number) {
        return orders.removeIf(order -> order.number() == number);
    }

    public synchronized void clear() {
        orders.clear();
    }
}
