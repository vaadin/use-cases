package com.example.data;

import java.time.LocalDate;

/**
 * A row of the demo data used by every use case in this module.
 *
 * @param id
 *            internal database id — the kind of column a report is expected to
 *            leave out (UC6)
 * @param name
 *            employee name
 * @param department
 *            department name, used for filtering in UC1
 * @param country
 *            two-letter country code
 * @param hired
 *            hire date, rendered by a {@code LocalDateRenderer} in UC4
 * @param salary
 *            annual salary, rendered by a {@code NumberRenderer} in UC4 and
 *            summed into the footer in UC5
 * @param bonus
 *            annual bonus, grouped with the salary under a joined header in UC5
 * @param active
 *            employment status, rendered as a {@code Checkbox} in UC4
 * @param cardNumber
 *            corporate card number — sensitive, masked on export in UC6
 */
public record Employee(int id, String name, String department, String country,
        LocalDate hired, double salary, double bonus, boolean active,
        String cardNumber) {
}
