package com.example.acme;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryTest {

    @Test
    void chipsAndTimingsCarryTheirStyleClasses() {
        assertTrue(Telemetry.chip("vaadin.rpc.duration").getClassNames()
                .contains("metric"));
        assertTrue(Telemetry.timing("12 ms").getClassNames()
                .contains("timing"));
    }

    @Test
    void highlightDurationsWrapsEveryDurationAndKeepsTheSentence() {
        String sentence = "The count query takes 1214 ms (max 1,219 ms), "
                + "over the 1000 ms budget. It counted 5,120 items.";

        Paragraph paragraph = Telemetry.highlightDurations(sentence);

        assertEquals(sentence, paragraph.getElement().getTextRecursively(),
                "highlighting must not alter the kit's own wording");
        List<String> timings = paragraph.getChildren()
                .filter(child -> child.getClassNames().contains("timing"))
                .map(Component::getElement).map(e -> e.getTextRecursively())
                .toList();
        assertEquals(List.of("1214 ms", "1,219 ms", "1000 ms"), timings,
                "every duration, and only durations: item counts stay plain");
    }

    @Test
    void highlightDurationsLeavesASentenceWithoutDurationsAsOneSpan() {
        Paragraph paragraph = Telemetry
                .highlightDurations("Nothing measured here.");

        assertEquals(1, paragraph.getChildren().count());
        assertTrue(paragraph.getChildren().findFirst()
                .orElseThrow() instanceof Span);
    }
}
