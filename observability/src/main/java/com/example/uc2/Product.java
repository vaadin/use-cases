package com.example.uc2;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * A product with a many-to-many {@code category} association, mapped through
 * the {@code product_category} join table.
 * <p>
 * This is the entity that reproduces the bookstore-example's N+1 join-table
 * fetch. The association is eager, so loading a <em>list</em> of products with
 * HQL ({@code from Product}, i.e. {@code ProductRepository.findAll()}) does
 * <strong>not</strong> join the categories in: Hibernate runs one query for the
 * products, then one more query <em>per product</em> to read that product's
 * rows out of the join table. N products therefore cost N+1 result-set fetches
 * — N of them tiny single-(product-)row collection fetches. That is exactly
 * what UC2's catalog-load button makes visible through the Observability Kit's
 * {@code vaadin.db.fetch.rows} meter.
 * <p>
 * The fix, deliberately left off here so the problem is observable, is one
 * annotation on the field below:
 *
 * <pre>
 * &#64;org.hibernate.annotations.BatchSize(size = 100)
 * private Set&lt;Category&gt; category = new HashSet&lt;&gt;();
 * </pre>
 *
 * With {@code @BatchSize}, Hibernate collapses those N per-product fetches into
 * a handful of batched {@code where product_id in (?, ?, …)} queries — the
 * statement count drops from N+1 to ~2, and the catalog-load readout shows it.
 * In the bookstore-example this annotation is present on
 * {@code Product.category}; removing it there resurrects the same N+1.
 */
@Entity
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    private String name = "";

    // No @BatchSize on purpose: each product's categories are fetched in their
    // own single-row-per-product query, so loading the catalog fires N+1
    // statements that UC2 surfaces. Add @BatchSize(size = 100) to fix it.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "product_category", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> category = new HashSet<>();

    public Product() {
    }

    public Product(String name) {
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

    public Set<Category> getCategory() {
        return category;
    }

    public void setCategory(Set<Category> category) {
        this.category = category;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || id == null) {
            return false;
        }
        if (obj instanceof Product other) {
            return id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id == null ? super.hashCode() : Objects.hash(id);
    }
}
