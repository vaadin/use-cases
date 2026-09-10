package com.example.uc8;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedMapSignal;

/**
 * UC8's shared table: the rows themselves, and who is looking at which one.
 * <p>
 * Rows are a {@link SharedListSignal} because a table has order and because two
 * users adding a row at the same time must both get theirs — {@code insertLast}
 * is an append, not a write of the whole collection. Who is where is a
 * {@link SharedMapSignal} keyed by peer, holding the row id: one entry per
 * user, replaced as they move, gone when they leave.
 */
@Component
public class RosterTopic {

    public record Employee(String id, String firstName, String lastName,
            String email) {

        public String fullName() {
            return firstName + " " + lastName;
        }
    }

    private final SharedListSignal<Employee> rows = new SharedListSignal<>(
            Employee.class);

    private final SharedMapSignal<String> watching = new SharedMapSignal<>(
            String.class);

    public SharedListSignal<Employee> rows() {
        return rows;
    }

    public SharedMapSignal<String> watching() {
        return watching;
    }

    @PostConstruct
    void seed() {
        if (rows.peek().isEmpty()) {
            rows.insertAllLast(java.util.List.of(
                    new Employee("e1", "Aria", "Bailey",
                            "aria.bailey@example.com"),
                    new Employee("e2", "Cooper", "Phillips",
                            "cooper.phillips@example.com"),
                    new Employee("e3", "Eleanor", "Price",
                            "eleanor.price@example.com"),
                    new Employee("e4", "Isaac", "Jones",
                            "isaac.jones@example.com")));
        }
    }
}
