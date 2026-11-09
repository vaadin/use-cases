package com.example.uc5;

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

@SpringBootTest
@ViewPackages(classes = InsertTemplateView.class)
class InsertTemplateViewTest extends SpringBrowserlessTest {

    // The editor starts with "Hi,\n\n\n\nThanks,\n".

    @Test
    void greetingIsInsertedAtTheCursorWithItsPlaceholderSelected() {
        navigate(InsertTemplateView.class);
        TextArea editor = find(TextArea.class).single();
        selectInBrowser(editor, 4, 4);

        Optional<Requested> selection = clickAndReadSelection(
                find(Button.class).withText("Greeting").single(), editor);

        assertEquals("Hi,\nHello {name}!\n\n\nThanks,\n", editor.getValue());
        assertEquals(Optional.of(new Requested(10, 16)), selection,
                "{name} should be selected");
    }

    @Test
    void snippetReplacesTheSelectedText() {
        navigate(InsertTemplateView.class);
        TextArea editor = find(TextArea.class).single();
        selectInBrowser(editor, 0, 3);

        clickAndReadSelection(
                find(Button.class).withText("Placeholder").single(), editor);

        assertEquals("{TODO}\n\n\n\nThanks,\n", editor.getValue());
    }

    @Test
    void signatureLeavesTheCursorAfterIt() {
        navigate(InsertTemplateView.class);
        TextArea editor = find(TextArea.class).single();
        int end = editor.getValue().length();
        selectInBrowser(editor, end, end);

        Optional<Requested> selection = clickAndReadSelection(
                find(Button.class).withText("Signature").single(), editor);

        assertEquals("Hi,\n\n\n\nThanks,\nBest regards,\nJamie",
                editor.getValue());
        int cursor = editor.getValue().length();
        assertEquals(Optional.of(new Requested(cursor, cursor)), selection);
    }
}
