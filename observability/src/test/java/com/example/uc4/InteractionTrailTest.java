package com.example.uc4;

import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The collector, driven through the real thing rather than a stubbed
 * {@code FinishedSpan}: what is being verified is not only that the class
 * stores spans but that this application is wired such that observations become
 * spans at all. Without the tracing bridge on the classpath every assertion
 * below fails with an empty trail, which is exactly the failure a unit test
 * with a hand-rolled span would hide.
 */
@SpringBootTest
class InteractionTrailTest {

    @Autowired
    ObservationRegistry observations;

    @Autowired
    Tracer tracer;

    @Autowired
    InteractionTrail trail;

    @Test
    void theTracingBridgeIsWiredSoObservationsBecomeSpans() {
        String traceId = inOneTrace("acme.test.wired", () -> {
        });

        assertNotNull(traceId,
                "no trace id means no tracer: with the bridge off the "
                        + "observation still runs and UC4 has nothing to read");
        assertEquals(1, trail.spans(traceId).size(),
                "the observation the application started must reach the "
                        + "SpanReporter as a finished span");
    }

    @Test
    void spansOfOneInteractionShareATraceAndNestByParent() {
        String traceId = inOneTrace("acme.test.dispatch",
                () -> observe("acme.test.carrier",
                        () -> observe("acme.test.wire", () -> {
                        })));

        List<InteractionTrail.Span> spans = trail.spans(traceId);
        assertEquals(3, spans.size());
        assertTrue(
                spans.stream().allMatch(span -> traceId.equals(span.traceId())),
                "one interaction is one trace, which is what makes the "
                        + "trail one trail");
        List<InteractionTrail.Span> roots = TrailTable.roots(spans);
        assertEquals(1, roots.size(), "a complete trail has one root");
        assertEquals("acme.test.dispatch", roots.get(0).name());
        InteractionTrail.Span carrier = named(spans, "acme.test.carrier");
        assertEquals(roots.get(0).spanId(), carrier.parentId(),
                "the inner hop must hang off the outer one, or the trail "
                        + "cannot be drawn as a tree");
        assertEquals(carrier.spanId(),
                named(spans, "acme.test.wire").parentId());
    }

    @Test
    void aFailingHopCarriesTheFailureAndItsEnclosingHopsDoNot() {
        String traceId = inOneTrace("acme.test.handled",
                () -> assertThrows(IllegalStateException.class,
                        () -> observe("acme.test.refused", () -> {
                            throw new IllegalStateException("no capacity");
                        })));

        List<InteractionTrail.Span> spans = trail.spans(traceId);
        InteractionTrail.Span refused = named(spans, "acme.test.refused");
        assertTrue(refused.failed());
        assertEquals("IllegalStateException: no capacity", refused.error());
        assertFalse(named(spans, "acme.test.handled").failed(),
                "a failure the caller handles leaves the enclosing span "
                        + "successful — the point UC4's step 3 makes");
    }

    @Test
    void spansCarryTheAttributesAMeterTagCouldNotHold() {
        String traceId = inOneTrace("acme.test.tagged", () -> Observation
                .createNotStarted("acme.test.tagged.inner", observations)
                .contextualName("acme.test.tagged.inner")
                .lowCardinalityKeyValue("acme.carrier", "OverNite")
                .highCardinalityKeyValue("acme.shipment", "SHP-04471")
                .observe(() -> {
                }));

        InteractionTrail.Span span = named(trail.spans(traceId),
                "acme.test.tagged.inner");
        assertEquals("OverNite", span.tags().get("acme.carrier"));
        assertEquals("SHP-04471", span.tags().get("acme.shipment"),
                "the high-cardinality attribution a meter tag may not carry "
                        + "is exactly what a span is for");
    }

    @Test
    void spansOfInteractionsNobodyFollowedAreDropped() {
        // Not followed: the poll requests that keep the readout live, every
        // other view's traffic, the heartbeats.
        String[] traceId = new String[1];
        Observation.createNotStarted("acme.test.unfollowed", observations)
                .contextualName("acme.test.unfollowed")
                .observe(() -> traceId[0] = tracer.currentSpan().context()
                        .traceId());

        assertTrue(trail.spans(traceId[0]).isEmpty());
        assertFalse(trail.followed().contains(traceId[0]),
                "the page must not accumulate traffic it is not explaining");
    }

    @Test
    void theNewestFollowedInteractionIsTheOneTheViewShows() {
        String first = inOneTrace("acme.test.first", () -> {
        });
        String second = inOneTrace("acme.test.second", () -> {
        });

        assertEquals(second, trail.newest().orElseThrow());
        assertTrue(trail.followed().indexOf(second) < trail.followed()
                .indexOf(first), "newest first");
    }

    @Test
    void onlyTheLastFewInteractionsAreKept() {
        String oldest = inOneTrace("acme.test.evicted", () -> {
        });
        for (int i = 0; i < InteractionTrail.MAX_TRAILS; i++) {
            inOneTrace("acme.test.newer", () -> {
            });
        }

        assertTrue(trail.spans(oldest).isEmpty(),
                "a bounded buffer is what keeps an in-process collector from "
                        + "being a leak");
        assertEquals(InteractionTrail.MAX_TRAILS, trail.followed().size());
    }

    @Test
    void spansAreCopiedOutOfTheTracersOwnModel() {
        // The Brave implementation backs a FinishedSpan with a span it may
        // recycle, while a trail outlives the request that produced it, so
        // the reading has to be a snapshot rather than a live view.
        String traceId = inOneTrace("acme.test.snapshot", () -> {
        });

        InteractionTrail.Span span = trail.spans(traceId).get(0);
        assertEquals(span, trail.spans(traceId).get(0),
                "two readings of one trail must describe the same span");
        assertThrows(UnsupportedOperationException.class,
                () -> span.tags().put("acme.carrier", "OverNite"),
                "the attributes are a snapshot, not a window into the "
                        + "tracer's own mutable span");
        assertNull(span.parentId(), "the only span of a trail is its root");
        assertTrue(span.durationMs() >= 0);
    }

    /**
     * Runs {@code work} inside one followed observation, the way the shipping
     * desk runs a dispatch.
     *
     * @return the trace id the work ran under
     */
    private String inOneTrace(String name, Runnable work) {
        String[] traceId = new String[1];
        Observation.createNotStarted(name, observations).contextualName(name)
                .observe(() -> {
                    traceId[0] = tracer.currentSpan().context().traceId();
                    trail.follow(traceId[0]);
                    work.run();
                });
        return traceId[0];
    }

    private void observe(String name, Runnable work) {
        Observation.createNotStarted(name, observations).contextualName(name)
                .observe(work);
    }

    private static InteractionTrail.Span named(
            List<InteractionTrail.Span> spans, String name) {
        return spans.stream().filter(span -> name.equals(span.name()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no span named " + name + " in " + spans));
    }
}
