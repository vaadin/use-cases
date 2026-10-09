package com.example.uc4;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PriceListEditorView.class)
class PriceListEditorViewTest extends SpringBrowserlessTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void viewRendersToolbarAndPriceList() {
        navigate(PriceListEditorView.class);
        runPendingSignalsTasks();

        assertEquals("UC4 — Undo history",
                findInView(H1.class).single().getText());
        assertEquals(3, findInView(ListItem.class).all().size());
        assertFalse(button("Undo").isEnabled());
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

        test(button("Undo")).click();
        runPendingSignalsTasks();
        assertEquals(3, prices().size());

        test(button("Undo")).click();
        runPendingSignalsTasks();
        assertEquals("Espresso machine — 499.00", prices().getFirst());
        assertFalse(button("Undo").isEnabled());
    }

    @Test
    void historyIsSharedWithinAUiButNotAcrossUis() {
        navigate(PriceListEditorView.class);
        UndoHistory first = context.getBean(UndoHistory.class);
        assertSame(first, context.getBean(UndoHistory.class));

        // A reload or another tab means a new UI, and a new history.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(PriceListEditorView.class);
        assertNotSame(first, context.getBean(UndoHistory.class));
    }

    private List<String> prices() {
        return findInView(ListItem.class).all().stream().map(ListItem::getText)
                .toList();
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }
}
