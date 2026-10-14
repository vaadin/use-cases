package com.example.usecase35;

import java.util.List;

import com.example.usecase35.Card.Priority;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase35View.class)
@WithMockUser
class UseCase35ViewTest extends SpringBrowserlessTest {

    @Test
    void seedCardsRender() {
        navigate(UseCase35View.class);
        runPendingSignalsTasks();

        UseCase35View view = (UseCase35View) getCurrentView();
        assertEquals(3, view.cards.peek().size());
        assertTrue(
                $view(Span.class).all().stream()
                        .anyMatch(s -> "To do (3 cards):".equals(s.getText())),
                "Count badge should match seed");
    }

    @Test
    void addCardFromForm() {
        navigate(UseCase35View.class);
        runPendingSignalsTasks();

        TextField title = $view(TextField.class).all().stream()
                .filter(f -> "New card title".equals(f.getLabel())).findFirst()
                .orElseThrow();
        test(title).setValue("Investigate flaky CI");
        Button add = $view(Button.class).all().stream()
                .filter(b -> "Add to top".equals(b.getText())).findFirst()
                .orElseThrow();
        test(add).click();
        runPendingSignalsTasks();

        UseCase35View view = (UseCase35View) getCurrentView();
        assertEquals(4, view.cards.peek().size());
        assertEquals("Investigate flaky CI",
                view.cards.peek().get(0).peek().title());
    }

    @Test
    void moveToPreservesValueSignalIdentity() {
        navigate(UseCase35View.class);
        runPendingSignalsTasks();

        UseCase35View view = (UseCase35View) getCurrentView();
        List<ValueSignal<Card>> before = view.cards.peek();
        ValueSignal<Card> firstEntry = before.get(0);

        // Move the top card down one slot — same ValueSignal must end up at
        // index 1, not be replaced.
        view.cards.moveTo(firstEntry, 1);
        runPendingSignalsTasks();

        assertSame(firstEntry, view.cards.peek().get(1),
                "moveTo must preserve the ValueSignal instance");
    }

    @Test
    void pullBacklogPrependsBatch() {
        navigate(UseCase35View.class);
        runPendingSignalsTasks();

        UseCase35View view = (UseCase35View) getCurrentView();
        Button pull = $view(Button.class).all().stream()
                .filter(b -> "Pull backlog from server".equals(b.getText()))
                .findFirst().orElseThrow();
        test(pull).click();
        runPendingSignalsTasks();

        // 3 seeded + 4 from server
        assertEquals(7, view.cards.peek().size());
        // First card now starts with "From server:"
        assertTrue(view.cards.peek().get(0).peek().title()
                .startsWith("From server:"));
        assertTrue(view.cards.peek().get(3).peek().title()
                .startsWith("From server:"));
        // Original seed card moved down
        assertEquals("Wire up the new dashboard",
                view.cards.peek().get(4).peek().title());
    }

    @Test
    void pasteInsertsBatchAtPositionTwo() {
        navigate(UseCase35View.class);
        runPendingSignalsTasks();

        UseCase35View view = (UseCase35View) getCurrentView();
        Button paste = $view(Button.class).all().stream()
                .filter(b -> b.getText() != null
                        && b.getText().startsWith("Paste 3 cards"))
                .findFirst().orElseThrow();
        test(paste).click();
        runPendingSignalsTasks();

        assertEquals(6, view.cards.peek().size());
        // Index 1, 2, 3 should be the pasted ones
        assertEquals("Pasted A", view.cards.peek().get(1).peek().title());
        assertEquals("Pasted B", view.cards.peek().get(2).peek().title());
        assertEquals("Pasted C", view.cards.peek().get(3).peek().title());
        // Index 0 is still the original first card
        assertEquals("Wire up the new dashboard",
                view.cards.peek().get(0).peek().title());
    }

    @Test
    void priorityIsSetFromForm() {
        navigate(UseCase35View.class);
        runPendingSignalsTasks();

        UseCase35View view = (UseCase35View) getCurrentView();
        // Default priority is MEDIUM
        TextField title = $view(TextField.class).all().stream()
                .filter(f -> "New card title".equals(f.getLabel())).findFirst()
                .orElseThrow();
        test(title).setValue("Test card");
        Button add = $view(Button.class).all().stream()
                .filter(b -> "Add to top".equals(b.getText())).findFirst()
                .orElseThrow();
        test(add).click();
        runPendingSignalsTasks();

        assertEquals(Priority.MEDIUM,
                view.cards.peek().get(0).peek().priority());
    }
}
