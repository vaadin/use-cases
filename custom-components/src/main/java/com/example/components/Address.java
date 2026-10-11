package com.example.components;

import java.util.stream.Stream;

/** A postal address. */
public record Address(String street, String postalCode, String city,
        String country) {

    public static final Address EMPTY = new Address("", "", "", "");

    /** One line, for lists and cards. */
    public String singleLine() {
        return String.join(", ",
                Stream.of(street, postalCode + " " + city, country)
                        .map(String::strip).filter(part -> !part.isEmpty())
                        .toList());
    }
}
