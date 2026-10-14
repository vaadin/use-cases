package com.example.uc5;

import java.time.Duration;
import java.util.List;

import com.example.ManualLatency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LatestResponseWinsView.class)
class LatestResponseWinsViewTest extends SpringBrowserlessTest {

    @Autowired
    private ManualLatency latency;

    @BeforeEach
    void resetLatency() {
        latency.reset();
    }

    @Test
    void viewRendersSearchField() {
        navigate(LatestResponseWinsView.class);

        assertEquals("UC5 — Latest response wins",
                findInView(H1.class).single().getText());
        assertEquals("Search products",
                findInView(TextField.class).single().getLabel());
    }

    @Test
    void shorterTermsAnswerMoreSlowly() {
        assertTrue(LatestResponseWinsView.latencyFor("la")
                .compareTo(LatestResponseWinsView.latencyFor("lap")) > 0);
        assertEquals(Duration.ofMillis(300),
                LatestResponseWinsView.latencyFor("laptop"));
    }

    @Test
    void lateAnswerForAnOlderTermIsDiscarded() {
        navigate(LatestResponseWinsView.class);
        typeQuickly("la", "lap");

        answerNewestFirst();

        assertEquals("Results for \"lap\"", showing());
        assertEquals(List.of("Laptop 13\"", "Laptop 15\"", "Laptop sleeve",
                "Laptop stand"), results());
        assertTrue(logLines().contains("\"la\" answered late — discarded"));
    }

    @Test
    void withoutDiscardingTheLateAnswerWins() {
        navigate(LatestResponseWinsView.class);
        test(findInView(Checkbox.class).single()).click();
        typeQuickly("la", "lap");

        answerNewestFirst();

        // The list now shows "la" results while the field says "lap".
        assertEquals("Results for \"la\"", showing());
        assertTrue(results().contains("Label printer"));
    }

    private void typeQuickly(String... values) {
        TextField search = findInView(TextField.class).single();
        for (String value : values) {
            test(search).setValue(value);
        }
        assertEquals(values.length, latency.pending().size());
    }

    private void answerNewestFirst() {
        latency.complete(latency.pending().size() - 1);
        runPendingSignalsTasks();
        latency.completePending();
        runPendingSignalsTasks();
    }

    private String showing() {
        return findInView(Span.class).all().stream()
                .filter(span -> span.getClassNames().contains("showing"))
                .findFirst().orElseThrow().getText();
    }

    private List<String> results() {
        return findInView(ListItem.class).all().stream().map(ListItem::getText)
                .toList();
    }

    private List<String> logLines() {
        return findInView(Span.class).all().stream().map(Span::getText)
                .toList();
    }
}
