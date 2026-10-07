package com.example.uc7;

import com.example.TextSelectionTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.TextArea;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SelectionToolbarView.class)
class SelectionToolbarViewTest extends SpringBrowserlessTest {

    @Test
    void viewRenders() {
        navigate(SelectionToolbarView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(h -> h.getText()
                .equals("UC7 — Selection-driven transform toolbar")));
    }

    @Test
    void buttonsDisabledWithoutSelectionEnabledWithSelection() {
        navigate(SelectionToolbarView.class);
        runPendingSignalsTasks();

        Button upper = find(Button.class).withText("UPPERCASE").single();
        Button lower = find(Button.class).withText("lowercase").single();
        assertFalse(upper.isEnabled(),
                "no selection: button should be disabled");
        assertFalse(lower.isEnabled());

        TextArea editor = find(TextArea.class).single();
        TextSelectionTestSupport.setSelection(editor, 0, 5);
        runPendingSignalsTasks();

        assertTrue(upper.isEnabled(),
                "with selection: button should be enabled");
        assertTrue(lower.isEnabled());
    }

    @Test
    void uppercaseTransformsTheSelection() {
        navigate(SelectionToolbarView.class);
        TextArea editor = find(TextArea.class).single();

        // Select "Click" (first 5 chars)
        TextSelectionTestSupport.setSelection(editor, 0, 5);
        runPendingSignalsTasks();

        Button upper = find(Button.class).withText("UPPERCASE").single();
        test(upper).click();
        runPendingSignalsTasks();

        assertTrue(editor.getValue().startsWith("CLICK"),
                "value should start with CLICK, was: " + editor.getValue());
    }

    @Test
    void quoteTransformWrapsTheSelection() {
        navigate(SelectionToolbarView.class);
        TextArea editor = find(TextArea.class).single();
        TextSelectionTestSupport.setSelection(editor, 0, 5);
        runPendingSignalsTasks();

        test(find(Button.class).withText("\"Quote\"").single()).click();
        runPendingSignalsTasks();

        assertTrue(editor.getValue().startsWith("\"Click\""),
                "value should start with the quoted run, was: "
                        + editor.getValue());
    }
}
