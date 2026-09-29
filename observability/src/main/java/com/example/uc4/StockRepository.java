package com.example.uc4;

import com.example.uc2.Product;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * The shipping desk's read of the same product table UC2's inventory page loads
 * — one counting query per shipment line, which is what puts a datastore hop
 * into UC4's trail.
 * <p>
 * Deliberately one statement per line rather than one {@code in (…)} query: the
 * trail should show the datastore being visited once per line, so the reader
 * can see how a per-item loop reads in a trace. It is the honest shape of a lot
 * of real code, and it is a fraction of a millisecond here — the pathological
 * version of the same shape is UC2's N+1.
 */
public interface StockRepository extends Repository<Product, Long> {

    /**
     * How many catalog products match a shipment line's description.
     *
     * @param item
     *            the line's item description
     * @return the matching product count
     */
    @Query("select count(p) from Product p "
            + "where lower(p.name) like lower(concat('%', :item, '%'))")
    long countMatching(@Param("item") String item);
}
