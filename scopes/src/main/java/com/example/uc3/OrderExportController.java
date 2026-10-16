package com.example.uc3;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.uc3.ExportPreferences.Column;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A plain Spring MVC endpoint — not a Vaadin view — that downloads the order
 * list as CSV using the columns and delimiter the user picked in
 * {@link OrderExportView}. It reads the same HTTP-session-scoped
 * {@link ExportPreferences} instance as the view.
 */
@RestController
public class OrderExportController {

    public static final String PATH = "uc3/orders.csv";

    record Order(String id, String customer, LocalDate date, BigDecimal total) {
    }

    static final List<Order> ORDERS = List.of(
            new Order("A-1001", "Nordic Bikes", LocalDate.of(2026, 9, 1),
                    new BigDecimal("1249.00")),
            new Order("A-1002", "Harbor Café", LocalDate.of(2026, 9, 3),
                    new BigDecimal("86.50")),
            new Order("A-1003", "Lumen Studio", LocalDate.of(2026, 9, 7),
                    new BigDecimal("430.20")));

    private final ExportPreferences preferences;

    public OrderExportController(ExportPreferences preferences) {
        this.preferences = preferences;
    }

    @GetMapping("/" + PATH)
    public ResponseEntity<String> exportOrders() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"orders.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(toCsv(preferences));
    }

    static String toCsv(ExportPreferences preferences) {
        List<Column> columns = preferences.getColumns().stream().sorted()
                .toList();
        String delimiter = preferences.getDelimiter();
        Stream<String> header = Stream.of(columns.stream().map(Column::label)
                .collect(Collectors.joining(delimiter)));
        Stream<String> rows = ORDERS.stream()
                .map(order -> columns.stream()
                        .map(column -> value(order, column))
                        .collect(Collectors.joining(delimiter)));
        return Stream.concat(header, rows)
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private static String value(Order order, Column column) {
        return switch (column) {
        case ORDER_ID -> order.id();
        case CUSTOMER -> order.customer();
        case DATE -> order.date().toString();
        case TOTAL -> order.total().toPlainString();
        };
    }
}
