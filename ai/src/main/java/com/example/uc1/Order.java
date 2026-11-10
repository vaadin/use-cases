package com.example.uc1;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A sales order of the fictional company the assistant works for.
 */
public record Order(String id, String customer, String product, int quantity,
        BigDecimal amount, Status status, LocalDate orderDate,
        LocalDate dueDate) {

    public enum Status {
        OPEN, SHIPPED, DELIVERED, CANCELLED
    }

    /**
     * An order is late when it is still open after its due date.
     */
    public boolean isLate(LocalDate today) {
        return status == Status.OPEN && dueDate.isBefore(today);
    }
}
