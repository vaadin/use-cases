package com.example.uc2;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.io.Serializable;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A product category. Mapped from {@link Product} through a join table; this is
 * the "many" side that Hibernate fetches once per product when the association
 * is not batched (see {@link Product#getCategory()}).
 */
@Entity
public class Category implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    private String name = "";

    public Category() {
    }

    public Category(String name) {
        this.name = name;
    }

    public @Nullable Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || id == null) {
            return false;
        }
        if (obj instanceof Category other) {
            return id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id == null ? super.hashCode() : Objects.hash(id);
    }
}
