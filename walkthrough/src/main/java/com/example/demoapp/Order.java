package com.example.demoapp;

import java.math.BigDecimal;
import java.util.List;

/** An order of the demo app the tours explain. */
public record Order(String number, String customer, String status,
        BigDecimal total) {

    /** A few orders to fill the demo app's grid. */
    public static List<Order> samples() {
        return List.of(
                new Order("1041", "Nordic Bikes", "New",
                        new BigDecimal("1290.00")),
                new Order("1040", "Café Lumi", "Shipped",
                        new BigDecimal("86.50")),
                new Order("1039", "Harbor Tools", "Packed",
                        new BigDecimal("432.10")),
                new Order("1038", "Pine & Co", "Shipped",
                        new BigDecimal("2150.00")),
                new Order("1037", "Blue Kiosk", "New",
                        new BigDecimal("59.90")));
    }
}
