package com.example.uc4;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.exporter.SpanReporter;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * A span exporter that exports to the page you are looking at.
 * <p>
 * {@link SpanReporter} is the seam every tracing backend plugs into: Zipkin,
 * Tempo and the OTLP exporters are all beans of this type, handed each span as
 * it finishes. UC4 registers one of its own so the trail of a single
 * interaction can be read inside the application, without a trace UI running
 * next to it — the spans are the same spans, and swapping this bean for an
 * exporter sends them to a real backend unchanged.
 * <p>
 * A real exporter keeps everything and lets the trace UI do the filtering. This
 * one keeps only what it is told to: the view calls {@link #follow(String)}
 * with the trace id of the interaction it is handling, and every span of that
 * trace — the ones the application opens, the ones the kit opens around the
 * request, the RPC and each JDBC query — is retained until the trail is evicted
 * by newer ones. Anything else, the poll requests that keep the readout live
 * included, is dropped as it arrives, so the page never accumulates traffic it
 * is not explaining.
 * <p>
 * Spans are copied into {@link Span} rather than retained as
 * {@link FinishedSpan}: the Brave implementation backs one with a mutable span
 * the tracer may recycle, and this trail outlives the request that produced it.
 * <p>
 * Application-scoped, like every other readout in this module: the trails are
 * the application's telemetry, not one session's, and a second browser tab
 * reads the same ones.
 */
@Component
public class InteractionTrail implements SpanReporter {

    /** How many followed interactions are kept; the oldest is evicted. */
    static final int MAX_TRAILS = 8;

    /**
     * One finished span of a trail.
     *
     * @param traceId
     *            the trace the span belongs to — the identifier that makes the
     *            trail one trail
     * @param spanId
     *            this span's id
     * @param parentId
     *            the enclosing span's id, or {@code null} for the trail's root
     * @param name
     *            the span name, which for an Observation is its contextual name
     *            ({@code vaadin.rpc.event}, {@code acme.carrier.book})
     * @param start
     *            when the span started
     * @param end
     *            when it finished
     * @param tags
     *            the span's attributes, the high-cardinality ones included —
     *            this is where the kit puts the event name, the target
     *            component and the SQL statement, none of which a meter tag may
     *            carry
     * @param error
     *            the failure recorded on the span, or {@code null}
     */
    public record Span(String traceId, String spanId, @Nullable String parentId,
            String name, Instant start, Instant end, Map<String, String> tags,
            @Nullable String error) {

        /** How long the span took, in milliseconds. */
        public double durationMs() {
            return Duration.between(start, end).toNanos() / 1_000_000.0;
        }

        public boolean failed() {
            return error != null;
        }
    }

    // Insertion-ordered so eviction drops the oldest followed trail, and
    // synchronized because spans are reported on whichever thread finished
    // them while the view reads them on a request thread.
    private final Map<String, List<Span>> trails = Collections
            .synchronizedMap(new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, List<Span>> eldest) {
                    return size() > MAX_TRAILS;
                }
            });

    /**
     * Starts keeping the spans of one trace. Called from the interaction being
     * followed, before the work whose spans are wanted: a span already finished
     * when this is called was never offered to this reporter and cannot be
     * recovered.
     *
     * @param traceId
     *            the trace id of the current interaction
     */
    public void follow(String traceId) {
        trails.putIfAbsent(traceId, new ArrayList<>());
    }

    @Override
    public void report(FinishedSpan span) {
        String traceId = span.getTraceId();
        if (traceId == null) {
            return;
        }
        synchronized (trails) {
            List<Span> spans = trails.get(traceId);
            if (spans == null) {
                // Not an interaction anyone asked to follow.
                return;
            }
            spans.add(copyOf(span, traceId));
        }
    }

    private static Span copyOf(FinishedSpan span, String traceId) {
        Throwable error = span.getError();
        return new Span(traceId, span.getSpanId(), span.getParentId(),
                span.getName(), span.getStartTimestamp(),
                span.getEndTimestamp(), Map.copyOf(span.getTags()),
                error == null ? null : describe(error));
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }

    /**
     * The spans of one followed trail, in the order they finished — so
     * innermost first, and the request span last, since a span cannot finish
     * before the work it encloses.
     *
     * @param traceId
     *            the trail's trace id
     * @return its spans, empty when the trail is not followed or nothing has
     *         finished yet
     */
    public List<Span> spans(String traceId) {
        synchronized (trails) {
            List<Span> spans = trails.get(traceId);
            return spans == null ? List.of() : List.copyOf(spans);
        }
    }

    /** The followed trace ids, newest first. */
    public List<String> followed() {
        synchronized (trails) {
            List<String> ids = new ArrayList<>(trails.keySet());
            Collections.reverse(ids);
            return ids;
        }
    }

    /** The newest followed trace id, if any interaction has been followed. */
    public Optional<String> newest() {
        return followed().stream().findFirst();
    }
}
