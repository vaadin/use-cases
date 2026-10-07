package com.example.acme;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

/**
 * The typography of telemetry in the use cases: meter and tag names render as
 * monospace chips, measured values as emphasized tabular numerals in the
 * theme's accent, so a reader recognizes "this is a meter", "this is a
 * duration" at a glance across every use case. The styles live in
 * {@code styles.css} under the class names used here.
 */
public final class Telemetry {

    /** Durations as the kit writes them, e.g. "1214 ms" or "1,214 ms". */
    private static final Pattern DURATION = Pattern.compile("\\d[\\d,]* ms");

    private Telemetry() {
    }

    /** A meter name, a tag, a component name: telemetry vocabulary. */
    public static Span chip(String value) {
        Span chip = new Span(value);
        chip.addClassName("metric");
        return chip;
    }

    /** A measured value: a duration, a count, a size. */
    public static Span timing(String value) {
        Span span = new Span(value);
        span.addClassName("timing");
        return span;
    }

    /**
     * A sentence with every duration in it rendered as a {@link #timing}, for
     * prose the kit writes itself (insight summaries) where the numbers are
     * the point.
     */
    public static Paragraph highlightDurations(String sentence) {
        Paragraph paragraph = new Paragraph();
        Matcher matcher = DURATION.matcher(sentence);
        int consumed = 0;
        while (matcher.find()) {
            paragraph.add(new Span(sentence.substring(consumed,
                    matcher.start())), timing(matcher.group()));
            consumed = matcher.end();
        }
        paragraph.add(new Span(sentence.substring(consumed)));
        return paragraph;
    }
}
