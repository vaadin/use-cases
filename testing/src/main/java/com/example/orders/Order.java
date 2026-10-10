package com.example.orders;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A placed order. */
public record Order(int number, String customer, Product product, int quantity,
        LocalDate delivery, BigDecimal total) {
}
