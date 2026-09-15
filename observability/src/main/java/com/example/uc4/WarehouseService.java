package com.example.uc4;

import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acme's warehouse: reserves the stock for a shipment's lines.
 * <p>
 * The hop the application has to name itself. The Observability Kit instruments
 * the framework — the request, the RPC, navigations, data provider queries, and
 * (with its database feature on) every JDBC statement — but "reserve stock" is
 * a step only this application knows about, and a trail without it would jump
 * straight from the RPC to three unexplained {@code vaadin.db.query} spans.
 * Wrapping the method in an {@link Observation} is what puts it on the trail,
 * and because the kit's request and RPC observations are already current on
 * this thread, the span nests under them without anything being passed around.
 */
@Service
public class WarehouseService {

    /** The observation, and therefore the span, this hop appears as. */
    public static final String OBSERVATION = "acme.warehouse.reserve";

    /**
     * What one reservation found.
     *
     * @param lines
     *            how many shipment lines were reserved
     * @param onHand
     *            how many catalog products backed them
     */
    public record Reservation(int lines, long onHand) {
    }

    private final StockRepository stock;
    private final ObservationRegistry observations;

    WarehouseService(StockRepository stock, ObservationRegistry observations) {
        this.stock = stock;
        this.observations = observations;
    }

    /**
     * Reserves a shipment's lines, one stock lookup per line.
     *
     * @param items
     *            the lines' item descriptions
     * @return what the lookups found
     */
    @Transactional(readOnly = true)
    public Reservation reserve(List<String> items) {
        return Observation.createNotStarted(OBSERVATION, observations)
                .contextualName(OBSERVATION)
                // Low cardinality: a shipment has a handful of lines, so this
                // is safe as a tag as well as a span attribute.
                .lowCardinalityKeyValue("acme.shipment.lines",
                        Integer.toString(items.size()))
                .observe(() -> {
                    long onHand = 0;
                    for (String item : items) {
                        onHand += stock.countMatching(item);
                    }
                    return new Reservation(items.size(), onHand);
                });
    }
}
