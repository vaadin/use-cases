package com.example.uc7;

import java.util.List;
import java.util.Optional;

import com.example.TextSelectionTestSupport.Requested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextArea;

import static com.example.TextSelectionTestSupport.clickAndReadSelection;
import static com.example.TextSelectionTestSupport.selectInBrowser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SelectionToolbarView.class)
class SelectionToolbarViewTest extends SpringBrowserlessTest {

    // The editor starts with
    // "Click and drag to select some text, then transform it."

    @Test
    void buttonsAreEnabledOnlyWhileTextIsSelected() {
        navigate(SelectionToolbarView.class);
        runPendingSignalsTasks();
        List<Button> buttons = findInView(Button.class).all();
        assertEquals(4, buttons.size());
        assertTrue(buttons.stream().noneMatch(Button::isEnabled));

        TextArea editor = find(TextArea.class).single();
        selectInBrowser(editor, 0, 5);
        runPendingSignalsTasks();
        assertTrue(buttons.stream().allMatch(Button::isEnabled));

        selectInBrowser(editor, 5, 5);
        runPendingSignalsTasks();
        assertTrue(buttons.stream().noneMatch(Button::isEnabled));
    }

    @Test
    void transformsReplaceTheSelectionAndSelectTheResultSoTheyChain() {
        navigate(SelectionToolbarView.class);
        TextArea editor = find(TextArea.class).single();
        selectInBrowser(editor, 0, 5);
        runPendingSignalsTasks();

        Optional<Requested> selection = clickAndReadSelection(
                find(Button.class).withText("UPPERCASE").single(), editor);
        assertTrue(editor.getValue().startsWith("CLICK and drag"),
                editor.getValue());
        assertEquals(Optional.of(new Requested(0, 5)), selection);

        // The browser reports the selection the server asked for.
        selectInBrowser(editor, 0, 5);
        selection = clickAndReadSelection(
                find(Button.class).withText("\"Quote\"").single(), editor);
        assertTrue(editor.getValue().startsWith("\"CLICK\" and drag"),
                editor.getValue());
        assertEquals(Optional.of(new Requested(0, 7)), selection);
    }
}
