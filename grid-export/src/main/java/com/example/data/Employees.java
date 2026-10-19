package com.example.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Deterministic generator for the demo {@link Employee} rows. The seed is fixed
 * so that the views, the tests and the exported files all agree on what the
 * data is.
 */
public final class Employees {

    private static final String[] FIRST_NAMES = { "Ada", "Grace", "Alan",
            "Linus", "Barbara", "Ken", "Margaret", "Dennis", "Radia", "Tim",
            "Anita", "Edsger", "Frances", "Guido", "Hedy", "Jean" };

    private static final String[] LAST_NAMES = { "Lovelace", "Hopper", "Turing",
            "Torvalds", "Liskov", "Thompson", "Hamilton", "Ritchie", "Perlman",
            "Berners-Lee", "Borg", "Dijkstra", "Allen", "Rossum", "Lamarr",
            "Bartik" };

    /** Departments, in the order UC1's filter suggestions use them. */
    public static final List<String> DEPARTMENTS = List.of("Engineering",
            "Design", "Sales", "Support", "Finance");

    private static final String[] COUNTRIES = { "FI", "DE", "US", "GB", "BR",
            "JP" };

    private Employees() {
    }

    /**
     * Builds {@code count} rows. The result is stable for a given count: row
     * {@code i} always looks the same.
     */
    public static List<Employee> sample(int count) {
        Random random = new Random(42);
        return IntStream.rangeClosed(1, count).mapToObj(id -> {
            String name = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                    + LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String department = DEPARTMENTS
                    .get(random.nextInt(DEPARTMENTS.size()));
            String country = COUNTRIES[random.nextInt(COUNTRIES.length)];
            LocalDate hired = LocalDate.of(2010, 1, 1)
                    .plusDays(random.nextInt(5000));
            double salary = 45_000 + random.nextInt(60) * 1000;
            double bonus = random.nextInt(20) * 500;
            boolean active = random.nextInt(10) > 1;
            String cardNumber = "4%03d %04d %04d %04d".formatted(
                    random.nextInt(1000), random.nextInt(10000),
                    random.nextInt(10000), random.nextInt(10000));
            return new Employee(id, name, department, country, hired, salary,
                    bonus, active, cardNumber);
        }).toList();
    }
}
