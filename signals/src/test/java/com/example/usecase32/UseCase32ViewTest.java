package com.example.usecase32;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase32View.class)
@WithMockUser
class UseCase32ViewTest extends SpringBrowserlessTest {

    @Test
    void bothFormsRenderInitialState() {
        navigate(UseCase32View.class);
        runPendingSignalsTasks();

        long matches = $view(Span.class).all().stream().filter(
                s -> "Summary: Alice Anderson <alice@example.com> [Free]"
                        .equals(s.getText()))
                .count();
        assertEquals(2L, matches, "Both forms should render the initial state");
    }

    @Test
    void editingTheMutableNameUpdatesTheJpaBean() {
        navigate(UseCase32View.class);
        runPendingSignalsTasks();

        UseCase32View view = (UseCase32View) getCurrentView();

        // First Name field belongs to the mutable form
        TextField name = $view(TextField.class).all().stream()
                .filter(f -> "Name".equals(f.getLabel())).findFirst()
                .orElseThrow();
        test(name).setValue("Brian Brown");
        runPendingSignalsTasks();

        assertEquals("Brian Brown", view.entitySignal.peek().getName());
        assertTrue(
                $view(Span.class).all().stream().anyMatch(
                        s -> ("Summary: Brian Brown <alice@example.com> [Free]")
                                .equals(s.getText())),
                "Mutable summary should reflect the edited name");
    }

    @Test
    void importButtonAppliesBatchAtomicallyOnBothSides() {
        navigate(UseCase32View.class);
        runPendingSignalsTasks();

        UseCase32View view = (UseCase32View) getCurrentView();

        // Capture run counts BEFORE the import to verify single-notification
        // behaviour after.
        int entityRunsBefore = Integer.parseInt(currentRunCountFor(0));
        int dtoRunsBefore = Integer.parseInt(currentRunCountFor(1));

        Button importBtn = $view(Button.class).all().stream()
                .filter(b -> b.getText() != null
                        && b.getText().startsWith("Import customer record"))
                .findFirst().orElseThrow();
        test(importBtn).click();
        runPendingSignalsTasks();

        assertEquals("Carol Chen", view.entitySignal.peek().getName());
        assertEquals("carol@example.com", view.entitySignal.peek().getEmail());
        assertEquals("Enterprise", view.entitySignal.peek().getPlan());

        assertEquals("Carol Chen", view.dtoSignal.peek().name());
        assertEquals("carol@example.com", view.dtoSignal.peek().email());
        assertEquals("Enterprise", view.dtoSignal.peek().plan());

        // Both sides should have fired exactly ONCE for the three-field
        // batch (otherwise we'd see +3 per side).
        int entityRunsAfter = Integer.parseInt(currentRunCountFor(0));
        int dtoRunsAfter = Integer.parseInt(currentRunCountFor(1));
        assertEquals(entityRunsBefore + 1, entityRunsAfter,
                "modify() must yield exactly one notification");
        assertEquals(dtoRunsBefore + 1, dtoRunsAfter,
                "update() must yield exactly one notification");
    }

    /**
     * Reads the Nth "Effect notifications: X" span (0 = mutable, 1 = DTO) and
     * returns just the numeric portion.
     */
    private String currentRunCountFor(int index) {
        var runs = $view(Span.class).all().stream()
                .filter(s -> s.getText() != null
                        && s.getText().startsWith("Effect notifications:"))
                .toList();
        String text = runs.get(index).getText();
        return text.substring("Effect notifications: ".length());
    }
}
