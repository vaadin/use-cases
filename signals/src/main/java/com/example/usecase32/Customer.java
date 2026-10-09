package com.example.usecase32;

import java.io.Serializable;

/**
 * Mutable customer bean — mirrors a typical JPA entity with setters that mutate
 * in place. Used to demonstrate the {@code ValueSignal.modify} /
 * {@code modifier()} pattern.
 */
class Customer implements Serializable {
    private String name;
    private String email;
    private String plan;

    Customer(String name, String email, String plan) {
        this.name = name;
        this.email = email;
        this.plan = plan;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getEmail() {
        return email;
    }

    void setEmail(String email) {
        this.email = email;
    }

    String getPlan() {
        return plan;
    }

    void setPlan(String plan) {
        this.plan = plan;
    }
}
