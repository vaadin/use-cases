package com.example.uc2;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Standard Spring Data repository. {@link #findAll()} issues a plain
 * {@code from Product} query; because {@link Product#getCategory()} is eager
 * and not batched, that one query fans out into a per-product join-table fetch
 * — the N+1 UC2 demonstrates. {@link #findAllWithCategories()} is the fix:
 * one query that brings the categories along.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Products with their categories in a single query, so the eager
     * association is already initialized and Hibernate has nothing left to
     * fetch per product.
     */
    @Query("select distinct p from Product p left join fetch p.category")
    List<Product> findAllWithCategories();
}
