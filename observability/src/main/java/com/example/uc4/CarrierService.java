package com.example.uc4;

import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Acme's freight carriers: books a shipment with one of them.
 * <p>
 * The backend service at the far end of the trail. Its latency is the demo
 * rig's to decide and its failure is the demo rig's to trigger, because the
 * point of UC4 is not that a booking is slow but that the trail says
 * <em>which</em> hop was.
 * <p>
 * The booking is faked with a sleep rather than an HTTP call, and that is the
 * one place this demo is less instrumented than a real application would be: a
 * call through Spring's {@code RestClient} or {@code WebClient} carries its own
 * observation, so a real carrier call would appear on the trail as a client
 * span with the URI and status on it, nested inside this one, and the trace
 * context would travel to the carrier in a {@code traceparent} header — which
 * is how a trail reaches past this process.
 */
@Service
public class CarrierService {

    /** The observation, and therefore the span, this hop appears as. */
    public static final String OBSERVATION = "acme.carrier.book";

    /** The carrier refused the booking. */
    public static class CarrierRefusedException extends RuntimeException {

        CarrierRefusedException(String carrier) {
            super(carrier + " refused the booking: no capacity on this lane");
        }
    }

    private final ObservationRegistry observations;
    // A singleton service books for every clerk at once, so the reference
    // counter is atomic rather than a plain int.
    private final AtomicInteger bookings = new AtomicInteger();

    CarrierService(ObservationRegistry observations) {
        this.observations = observations;
    }

    /**
     * Books a shipment with a carrier.
     *
     * @param carrier
     *            the carrier's name — low cardinality, so it is safe on the
     *            span and on a meter tag alike
     * @param latencyMs
     *            how long the carrier's API takes to answer, from the demo rig
     * @param refuse
     *            whether the carrier refuses the booking, from the demo rig
     * @return the carrier's booking reference
     * @throws CarrierRefusedException
     *             when the carrier refuses — deliberately let out of the
     *             observation, so the span records the failure before the
     *             shipping desk handles it
     */
    public String book(String carrier, @Nullable Integer latencyMs,
            boolean refuse) {
        return Observation.createNotStarted(OBSERVATION, observations)
                .contextualName(OBSERVATION)
                .lowCardinalityKeyValue("acme.carrier", carrier).observe(() -> {
                    sleep(latencyMs);
                    if (refuse) {
                        throw new CarrierRefusedException(carrier);
                    }
                    return "%s-%06d".formatted(
                            carrier.substring(0, 2).toUpperCase(),
                            81_204 + bookings.incrementAndGet());
                });
    }

    private static void sleep(@Nullable Integer millis) {
        if (millis == null || millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
