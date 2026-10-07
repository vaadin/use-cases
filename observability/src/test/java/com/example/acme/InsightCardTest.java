package com.example.acme;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightCardTest {

    private static final Map<String, Object> WARNING = Map.of("type",
            "slow-data-query", "severity", "warning", "summary",
            "The count query for ComboBox takes 1212 ms, over the 1000 ms "
                    + "budget.",
            "evidence", Map.of("route", "orders"));

    @Test
    void theCardAndItsPillCarryTheSeveritysAuraAccent() {
        InsightCard card = new InsightCard(WARNING, List.of());

        assertTrue(card.getClassNames().contains("verdict-card"));
        assertTrue(card.getClassNames().contains("v-warning"),
                "the Aura accent class sets the amber tokens for the card");
        Span pill = card.getChildren().filter(Span.class::isInstance)
                .map(Span.class::cast).findFirst().orElseThrow();
        assertEquals("warning", pill.getText());
        assertTrue(pill.getClassNames().contains("v-warning"),
                "the pill re-applies the accent since Aura resets it for "
                        + "children");
    }

    @Test
    void errorsReadRed() {
        Map<String, Object> error = Map.of("severity", "error", "summary",
                "boom");

        assertTrue(new InsightCard(error, List.of()).getClassNames()
                .contains("v-error"));
    }

    @Test
    void theSummaryIsTheKitsSentenceWithDurationsHighlighted() {
        InsightCard card = new InsightCard(WARNING, List.of());

        String text = card.getElement().getTextRecursively();
        assertTrue(text.contains("takes 1212 ms, over the 1000 ms budget"),
                "the kit's wording is shown verbatim");
        long highlighted = card.getElement().getChildren()
                .flatMap(e -> e.getChildren())
                .filter(e -> e.getClassList().contains("timing")).count();
        assertEquals(2, highlighted);
    }

    @Test
    void theChipsAreTheEvidenceTheViewChose() {
        InsightCard card = new InsightCard(WARNING,
                List.of("route=orders", "ComboBox", "3×"));

        List<String> chips = card.getElement().getChildren()
                .filter(e -> e.getClassList().contains("verdict-evidence"))
                .flatMap(e -> e.getChildren())
                .map(e -> e.getTextRecursively()).toList();
        assertEquals(List.of("route=orders", "ComboBox", "3×"), chips);
    }
}
