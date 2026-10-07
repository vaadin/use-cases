package com.example.uc5;

import com.example.TextSelectionTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.TextArea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = InsertTemplateView.class)
class InsertTemplateViewTest extends SpringBrowserlessTest {

    @Test
    void viewRenders() {
        navigate(InsertTemplateView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC5 — Insert template at cursor".equals(h.getText())));
    }

    @Test
    void greetingInsertsAtCurrentCursor() {
        navigate(InsertTemplateView.class);
        TextArea editor = find(TextArea.class).single();
        // Position the cursor at offset 4 ("Hi,\n" + here)
        TextSelectionTestSupport.setSelection(editor, 4, 4);
        runPendingSignalsTasks();

        String original = editor.getValue();
        Button greeting = find(Button.class).withText("Greeting").single();
        test(greeting).click();
        runPendingSignalsTasks();

        String expected = original.substring(0, 4) + "Hello {name}!"
                + original.substring(4);
        assertEquals(expected, editor.getValue());
    }

    @Test
    void snippetReplacesNonEmptySelection() {
        navigate(InsertTemplateView.class);
        TextArea editor = find(TextArea.class).single();
        // Default value is "Hi,\n\n\n\nThanks,\n". Select "Hi," (offsets 0..3).
        TextSelectionTestSupport.setSelection(editor, 0, 3);
        runPendingSignalsTasks();

        String original = editor.getValue();
        Button greeting = find(Button.class).withText("Greeting").single();
        test(greeting).click();
        runPendingSignalsTasks();

        assertEquals("Hello {name}!" + original.substring(3), editor.getValue(),
                "selected run should have been replaced by the snippet");
    }

    @Test
    void signatureInsertsAtStart() {
        navigate(InsertTemplateView.class);
        TextArea editor = find(TextArea.class).single();
        TextSelectionTestSupport.setSelection(editor, 0, 0);
        runPendingSignalsTasks();

        String original = editor.getValue();
        Button signature = find(Button.class).withText("Signature").single();
        test(signature).click();
        runPendingSignalsTasks();

        assertTrue(editor.getValue().startsWith("Best regards,\nJamie"),
                "value should start with the inserted signature, was: "
                        + editor.getValue());
        assertEquals("Best regards,\nJamie".length() + original.length(),
                editor.getValue().length());
    }
}
