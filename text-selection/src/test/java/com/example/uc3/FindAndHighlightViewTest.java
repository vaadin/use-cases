package com.example.uc3;

import java.util.Optional;

import com.example.TextSelectionTestSupport.Requested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import static com.example.TextSelectionTestSupport.clickAndReadSelection;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = FindAndHighlightView.class)
class FindAndHighlightViewTest extends SpringBrowserlessTest {

    @Test
    void findNextSelectsEachMatchInTurnAndWrapsToTheFirst() {
        navigate(FindAndHighlightView.class);
        TextArea content = find(TextArea.class).single();
        Button findNext = find(Button.class).withText("Find next").single();
        test(find(TextField.class).single()).setValue("dolor");

        String text = content.getValue();
        int first = text.indexOf("dolor");
        int second = text.indexOf("dolor", first + 1);
        int third = text.indexOf("dolor", second + 1);
        int fourth = text.indexOf("dolor", third + 1);
        assertEquals(-1, text.indexOf("dolor", fourth + 1),
                "the sample text should have four matches");

        for (int match : new int[] { first, second, third, fourth }) {
            assertEquals(Optional.of(new Requested(match, match + 5)),
                    clickAndReadSelection(findNext, content));
            assertEquals("Match at " + match, status());
        }
        assertEquals(Optional.of(new Requested(first, first + 5)),
                clickAndReadSelection(findNext, content));
        assertEquals("Wrapped to start — match at " + first, status());
    }

    @Test
    void aNewSearchTermStartsFromTheTop() {
        navigate(FindAndHighlightView.class);
        TextArea content = find(TextArea.class).single();
        TextField search = find(TextField.class).single();
        Button findNext = find(Button.class).withText("Find next").single();
        test(search).setValue("dolor");
        clickAndReadSelection(findNext, content);
        clickAndReadSelection(findNext, content);

        test(search).setValue("ut");

        int first = content.getValue().indexOf("ut");
        assertEquals(Optional.of(new Requested(first, first + 2)),
                clickAndReadSelection(findNext, content));
    }

    @Test
    void emptySearchTermSelectsNothingAndShowsAHint() {
        navigate(FindAndHighlightView.class);
        TextArea content = find(TextArea.class).single();

        assertEquals(Optional.empty(), clickAndReadSelection(
                find(Button.class).withText("Find next").single(), content));
        assertEquals("Enter a search term first", status());
    }

    private String status() {
        return find(Span.class).withClassName("uc-status").single().getText();
    }
}
