package com.example.usecase32;

import java.io.Serializable;

/**
 * Immutable customer DTO — the API/wire representation with {@code withX}
 * builders. Used to demonstrate the {@code ValueSignal.update} /
 * {@code updater()} pattern.
 */
record CustomerDto(String name, String email,
        String plan) implements Serializable {
    CustomerDto withName(String name) {
        return new CustomerDto(name, email, plan);
    }

    CustomerDto withEmail(String email) {
        return new CustomerDto(name, email, plan);
    }

    CustomerDto withPlan(String plan) {
        return new CustomerDto(name, email, plan);
    }
}
