package com.example.uc4;

import java.util.List;

import com.example.common.BuiltWith;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { PriceListEditorView.class, HomeView.class })
class PriceListEditorViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersToolbarAndPriceList() {
        navigate(PriceListEditorView.class);
        runPendingSignalsTasks();

        assertEquals("UC4 — Undo history",
                findInView(H1.class).single().getText());
        assertEquals(3, prices().size());
        assertFalse(button("Undo").isEnabled());
        assertTrue(emptyHistoryHint().isVisible());
    }

    @Test
    void toolbarUndoesChangesMadeInTheEditor() {
        navigate(PriceListEditorView.class);
        runPendingSignalsTasks();

        test(button("Raise all prices by 10%")).click();
        test(button("Remove last product")).click();
        runPendingSignalsTasks();
        assertEquals(
                List.of("Espresso machine — 548.90", "Coffee grinder — 141.90"),
                prices());
        assertTrue(findInView(Span.class).all().stream().anyMatch(
                s -> s.getText().equals("Last change: Removed the last product "
                        + "(2 in history)")));
        assertEquals(
                List.of("Raised all prices by 10%", "Removed the last product"),
                history());

        test(button("Undo")).click();
        runPendingSignalsTasks();
        assertEquals(3, prices().size());
        assertEquals(List.of("Raised all prices by 10%"), history());

        test(button("Undo")).click();
        runPendingSignalsTasks();
        assertEquals("Espresso machine — 499.00", prices().getFirst());
        assertFalse(button("Undo").isEnabled());
        assertEquals(List.of(), history());
        assertTrue(emptyHistoryHint().isVisible());
    }

    @Test
    void undoAfterNavigatingAwayAndBackRestoresTheListOnScreen() {
        navigate(PriceListEditorView.class);
        runPendingSignalsTasks();
        test(button("Remove last product")).click();

        // Same UI, new view instance: the UI-scoped list and history are both
        // kept. A second UI in the same session (another tab) can not be
        // simulated in browserless tests; see API-GAPS.md #2.
        navigate(HomeView.class);
        navigate(PriceListEditorView.class);
        runPendingSignalsTasks();
        assertEquals(2, prices().size());
        assertEquals(List.of("Removed the last product"), history());

        test(button("Undo")).click();
        runPendingSignalsTasks();
        assertEquals(3, prices().size());
    }

    private List<String> prices() {
        return items(UnorderedList.class);
    }

    private Paragraph emptyHistoryHint() {
        return findInView(Paragraph.class).all().stream()
                .filter(p -> p.getText().equals("No changes yet in this UI."))
                .findFirst().orElseThrow();
    }

    private List<String> history() {
        return items(OrderedList.class);
    }

    private List<String> items(Class<? extends HtmlContainer> listType) {
        // Not the points of "How it is built".
        HtmlContainer list = findInView(listType).all().stream()
                .filter(l -> l.findAncestor(BuiltWith.class) == null)
                .reduce((a, b) -> {
                    throw new IllegalStateException("More than one list");
                }).orElseThrow();
        return find(ListItem.class).from(list).all().stream()
                .map(ListItem::getText).toList();
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }
}
