package com.example.orders;

import java.math.BigDecimal;

/** The products the order desk sells. */
public enum Product {

    ESPRESSO_BEANS("Espresso beans, 1 kg", "24.90"),
    GRINDER("Burr grinder", "189.00"),
    PAPER_CUPS("Paper cups, box of 1000", "38.75");

    private final String title;
    private final BigDecimal price;

    Product(String title, String price) {
        this.title = title;
        this.price = new BigDecimal(price);
    }

    public String title() {
        return title;
    }

    public BigDecimal price() {
        return price;
    }
}
