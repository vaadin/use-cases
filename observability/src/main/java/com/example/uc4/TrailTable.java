package com.example.uc4;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.acme.Telemetry;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;

/**
 * One interaction's trail, drawn the way a trace UI draws it: the spans nested
 * by parent, each on a timeline of the whole trail, with the attributes that
 * say what the span was doing.
 * <p>
 * A plain HTML table, like every other readout in this module, because the kit
 * instruments in-memory data providers too and a {@code Grid} showing the trail
 * would record data queries — and, with tracing on, open spans — on the very
 * route whose trail it displays.
 */
public class TrailTable extends Table {

    /** Attributes not worth a chip: shown by the row's own styling instead. */
    private static final Set<String> REDUNDANT = Set.of("error");

    /** Long attribute values (a SQL statement) are cut to this. */
    private static final int MAX_VALUE = 60;

    public TrailTable() {
        addClassName("trail-table");
        setWidthFull();
        addHeaderRow("Span", "Timeline", "Duration", "Attributes");
    }

    /**
     * Replaces the trail.
     *
     * @param spans
     *            the trail's spans, in any order
     */
    public void setTrail(List<InteractionTrail.Span> spans) {
        List.copyOf(getBody().getRows()).forEach(TableRow::removeFromParent);
        if (spans.isEmpty()) {
            return;
        }
        Instant start = spans.stream().map(InteractionTrail.Span::start)
                .min(Comparator.naturalOrder()).orElseThrow();
        Instant end = spans.stream().map(InteractionTrail.Span::end)
                .max(Comparator.naturalOrder()).orElseThrow();
        // Floored, so a trail whose spans all started and ended within the
        // same instant still divides.
        double totalMs = Math.max(0.001,
                Duration.between(start, end).toNanos() / 1_000_000.0);

        Map<String, List<InteractionTrail.Span>> children = spans.stream()
                .filter(span -> span.parentId() != null).collect(
                        Collectors.groupingBy(InteractionTrail.Span::parentId));
        roots(spans).forEach(root -> render(root, 0, children, start, totalMs));
    }

    /**
     * The spans no other span in the trail encloses. Normally one — the request
     * span — but a trail read before its root has finished has as many roots as
     * it has orphans, and they are all worth showing.
     *
     * @param spans
     *            the trail's spans
     * @return its roots, earliest first
     */
    static List<InteractionTrail.Span> roots(
            List<InteractionTrail.Span> spans) {
        Set<String> ids = spans.stream().map(InteractionTrail.Span::spanId)
                .collect(Collectors.toCollection(HashSet::new));
        return spans.stream()
                .filter(span -> span.parentId() == null
                        || !ids.contains(span.parentId()))
                .sorted(Comparator.comparing(InteractionTrail.Span::start))
                .toList();
    }

    private void render(InteractionTrail.Span span, int depth,
            Map<String, List<InteractionTrail.Span>> children,
            Instant trailStart, double totalMs) {
        TableRow row = getBody().addRow();
        if (span.failed()) {
            row.addClassNames("trail-failed", "v-error");
        }
        row.addDataCell(name(span, depth));
        row.addDataCell(timeline(span, trailStart, totalMs));
        row.addDataCell(
                Telemetry.timing("%.1f ms".formatted(span.durationMs())));
        row.addDataCell(attributes(span));

        children.getOrDefault(span.spanId(), List.of()).stream()
                .sorted(Comparator.comparing(InteractionTrail.Span::start))
                .forEach(child -> render(child, depth + 1, children, trailStart,
                        totalMs));
    }

    /** The span's name, indented by its depth, with its failure under it. */
    private static Div name(InteractionTrail.Span span, int depth) {
        Div cell = new Div(Telemetry.chip(span.name()));
        cell.addClassName("trail-name");
        // Indentation by depth rather than by nested tables: the rows have to
        // stay one flat list so every timeline shares a column.
        cell.getStyle().set("padding-inline-start",
                "%.2frem".formatted(depth * 1.1));
        String error = span.error();
        if (error != null) {
            Span failure = new Span(error);
            failure.addClassName("trail-error");
            cell.add(failure);
        }
        return cell;
    }

    /**
     * The span's place in the trail: a bar starting where the span started and
     * as wide as it lasted, so nesting and overlap are visible at a glance —
     * the one thing a list of durations cannot show.
     */
    private static Div timeline(InteractionTrail.Span span, Instant trailStart,
            double totalMs) {
        double offset = Duration.between(trailStart, span.start()).toNanos()
                / 1_000_000.0;
        Div bar = new Div();
        bar.addClassName("trail-bar");
        bar.getStyle().set("margin-inline-start",
                "%.3f%%".formatted(100 * offset / totalMs));
        // A sub-millisecond span still has to be visible, hence the floor.
        bar.getStyle().set("width", "max(2px, %.3f%%)"
                .formatted(100 * span.durationMs() / totalMs));
        Div track = new Div(bar);
        track.addClassName("trail-track");
        return track;
    }

    /** The span's attributes as chips, the long ones cut to fit. */
    private static Div attributes(InteractionTrail.Span span) {
        Div chips = new Div();
        chips.addClassName("trail-attributes");
        List<String> keys = new ArrayList<>(span.tags().keySet());
        keys.sort(Comparator.naturalOrder());
        keys.stream().filter(key -> !REDUNDANT.contains(key))
                .forEach(key -> chips.add(Telemetry.chip(key + "="
                        + abbreviate(span.tags().getOrDefault(key, "")))));
        return chips;
    }

    private static String abbreviate(String value) {
        String collapsed = value.replaceAll("\\s+", " ").trim();
        return collapsed.length() <= MAX_VALUE ? collapsed
                : collapsed.substring(0, MAX_VALUE - 1) + "…";
    }
}
