package com.example.uc12;

import java.util.List;

import com.example.ManualLatency;
import com.example.backend.ReviewSummaries;
import com.example.uc12.StructuredSkeletonView.Section;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = StructuredSkeletonView.class)
class StructuredSkeletonViewTest extends SpringBrowserlessTest {

    @Autowired
    private ManualLatency latency;

    @BeforeEach
    void resetLatency() {
        latency.reset();
    }

    @Test
    void wholeSummaryStartsAsPlaceholders() {
        navigate(StructuredSkeletonView.class);

        assertEquals("UC12 — Skeleton that fills in",
                findInView(H1.class).single().getText());
        assertEquals(1, shownBars("bar-title"));
        assertEquals(1, shownBars("bar-rating"));
        assertEquals(2 * StructuredSkeletonView.EXPECTED_ITEMS,
                shownBars("bar-item"));
        assertEquals(ReviewSummaries.FIRST_PART,
                latency.pending().getFirst().delay());
    }

    @Test
    void eachPartReplacesItsOwnPlaceholders() {
        StructuredSkeletonView view = navigate(StructuredSkeletonView.class);

        nextPart(); // headline
        assertEquals("Barista espresso grinder",
                findInView(H2.class).single().getText());
        assertEquals(0, shownBars("bar-title"));
        assertEquals(1, shownBars("bar-rating"),
                "later sections keep their placeholders");

        nextPart(); // score
        assertEquals(0, shownBars("bar-rating"));

        nextPart(); // first of four pros
        assertEquals(List.of("Even grind from espresso to French press"),
                items());
        assertEquals(2 + 3, shownBars("bar-item"));

        nextPart();
        nextPart();
        // More than expected: one line stays while more may follow.
        assertEquals(3, items().size());
        assertEquals(1 + 3, shownBars("bar-item"));
        nextPart();
        assertEquals(1 + 3, shownBars("bar-item"));

        nextPart(); // first con: the pros are complete
        assertEquals(0 + 2, shownBars("bar-item"));

        nextPart();
        nextPart(); // done
        assertEquals(Section.DONE, view.section());
        assertEquals(0, shownBars("bar-item"));
        assertEquals(6, items().size());
        assertTrue(latency.pending().isEmpty());
    }

    @Test
    void summarisingAgainDropsTheOlderAnswer() {
        StructuredSkeletonView view = navigate(StructuredSkeletonView.class);
        nextPart();

        test(button("Summarise again")).click();
        assertEquals(Section.HEADLINE, view.section());
        assertEquals(1, shownBars("bar-title"));

        // The older summary's next part answers late: it must not show up.
        latency.completeNext();
        runPendingSignalsTasks();
        assertEquals(1, shownBars("bar-title"));
        assertEquals(1, latency.pending().size(),
                "the older summary must stop asking for parts");
    }

    @Test
    void otherProductHasItsOwnListLengths() {
        StructuredSkeletonView view = navigate(StructuredSkeletonView.class);
        @SuppressWarnings("unchecked")
        Select<String> product = findInView(Select.class).single();
        test(product).selectItem("Trail running shoes");

        while (view.section() != Section.DONE) {
            nextPart();
        }
        assertEquals("Trail running shoes",
                findInView(H2.class).single().getText());
        assertEquals(5, items().size());
    }

    @Test
    void leavingTheViewStopsTheSummary() {
        StructuredSkeletonView view = navigate(StructuredSkeletonView.class);
        nextPart();

        view.removeFromParent();
        latency.completeNext();
        assertTrue(latency.pending().isEmpty(),
                "a detached view must not keep asking for parts");
    }

    private void nextPart() {
        latency.completeNext();
        runPendingSignalsTasks();
    }

    private long shownBars(String kind) {
        return find(Div.class).all().stream()
                .filter(div -> div.getClassNames().contains(kind))
                .filter(StructuredSkeletonViewTest::shown).count();
    }

    private List<String> items() {
        return find(ListItem.class).all().stream().map(ListItem::getText)
                .toList();
    }

    private Button button(String text) {
        return find(Button.class).all().stream()
                .filter(button -> text.equals(button.getText())).findFirst()
                .orElseThrow();
    }

    private static boolean shown(Component component) {
        for (Component c = component; c != null; c = c.getParent()
                .orElse(null)) {
            if (!c.isVisible()) {
                return false;
            }
        }
        return true;
    }
}
