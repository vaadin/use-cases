package com.example.backend;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row of the generated order book. */
public record Order(long id, String customer, String product, int quantity,
        BigDecimal total, LocalDate ordered) {
}
