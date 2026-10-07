package com.example.uc2;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the product catalog. The plain load deliberately fans out into the N+1
 * join-table fetch (see {@link Product}); the join-fetching load is the fix.
 * How that fan-out is <em>observed</em> is not this service's concern — it
 * issues the queries, and the Observability Kit's database feature
 * ({@code vaadin.observability.database=true}) records each JDBC result-set
 * fetch into the {@code vaadin.db.fetch.rows} summary and a
 * {@code vaadin.db.query} span, with no code here touching the driver.
 */
@Service
public class ProductCatalogService {

    /** One product as the inventory page shows it. */
    public record Line(String name, String categories) {
    }

    /**
     * What one catalog load returned (the cost is read from the kit's meters).
     */
    public record CatalogLoad(List<Line> lines, int categories) {

        public int products() {
            return lines.size();
        }
    }

    private final ProductRepository products;

    public ProductCatalogService(ProductRepository products) {
        this.products = products;
    }

    /** The N+1 load: one query for the products, then one per product. */
    @Transactional(readOnly = true)
    public CatalogLoad loadCatalog() {
        return loadCatalog(false);
    }

    /**
     * Loads every product and its categories, as one unit of work.
     *
     * @param joinFetch
     *            {@code true} to bring the categories along in the product
     *            query — the fix; {@code false} for the eager, unbatched
     *            association's one-query-per-product fan-out
     */
    @Transactional(readOnly = true)
    public CatalogLoad loadCatalog(boolean joinFetch) {
        List<Product> all = joinFetch ? products.findAllWithCategories()
                : products.findAll();
        int categories = 0;
        List<Line> lines = new java.util.ArrayList<>();
        for (Product product : all) {
            // With the eager, unbatched association the per-product join-table
            // query has already run during findAll, which is what the kit's
            // fetch meter counts; with the join fetch there is nothing left.
            categories += product.getCategory().size();
            lines.add(new Line(product.getName(),
                    product.getCategory().stream().map(Category::getName)
                            .sorted().collect(Collectors.joining(", "))));
        }
        lines.sort(java.util.Comparator.comparing(Line::name));
        return new CatalogLoad(List.copyOf(lines), categories);
    }
}
