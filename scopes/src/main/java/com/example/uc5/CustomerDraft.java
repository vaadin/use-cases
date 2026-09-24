package com.example.uc5;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;
import com.vaadin.flow.spring.annotation.SpringComponent;

/**
 * The customer being created in the multi-page editor.
 * <p>
 * {@code @RouteScope} ties the bean's lifetime to navigation.
 * {@code @RouteScopeOwner(CustomerEditorLayout.class)} says the draft lives as
 * long as the editor layout is part of the current route chain: every page
 * inside the editor gets the same draft, and leaving the editor discards it.
 * The next visit starts a fresh draft.
 */
@SpringComponent
@RouteScope
@RouteScopeOwner(CustomerEditorLayout.class)
public class CustomerDraft {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final int number = COUNTER.incrementAndGet();
    private final LocalTime startedAt = LocalTime.now();

    private String name = "";
    private String email = "";
    private String street = "";
    private String city = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String describe() {
        return "Draft #" + number + ", started at "
                + startedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
