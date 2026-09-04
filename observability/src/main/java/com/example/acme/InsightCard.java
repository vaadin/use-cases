package com.example.acme;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

/**
 * One finding of the kit's insights endpoint as a card: a severity pill, the
 * kit's own summary sentence with its durations highlighted, an optional
 * detail line (a stack frame, an exception message), and a row of chips with
 * the evidence the view chose to surface. The card and the pill carry the
 * severity's Aura accent class, so a warning reads amber and an error red in
 * either theme.
 */
public class InsightCard extends Div {

    /**
     * @param insight
     *            the insight as the endpoint serializes it
     * @param chips
     *            the evidence to show as chips, already formatted — which
     *            evidence matters differs per insight type, so the view
     *            decides (route, component, event, occurrences, …)
     */
    public InsightCard(Map<String, Object> insight, List<String> chips) {
        this(insight, null, chips);
    }

    /**
     * @param insight
     *            the insight as the endpoint serializes it
     * @param detail
     *            one line of monospace detail under the summary — typically
     *            the application stack frame the kit attributes the finding
     *            to — or {@code null} for none
     * @param chips
     *            the evidence to show as chips, already formatted
     */
    public InsightCard(Map<String, Object> insight, @Nullable String detail,
            List<String> chips) {
        String severity = Insights.text(insight.get("severity"));
        String accent = Insights.accentOf(severity);

        Span pill = new Span(severity);
        pill.addClassNames("verdict-severity", accent);

        Paragraph summary = Telemetry
                .highlightDurations(Insights.text(insight.get("summary")));
        summary.addClassName("verdict-summary");

        addClassNames("verdict-card", accent);
        add(pill, summary);

        if (detail != null) {
            Span line = new Span(detail);
            line.addClassName("verdict-detail");
            add(line);
        }

        Div evidence = new Div();
        evidence.addClassName("verdict-evidence");
        chips.forEach(chip -> evidence.add(Telemetry.chip(chip)));
        add(evidence);
    }
}
