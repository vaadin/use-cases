package com.example.uc3;

import com.example.TextSelectionTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = FindAndHighlightView.class)
class FindAndHighlightViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndSampleContent() {
        navigate(FindAndHighlightView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(h -> h.getText()
                .equals("UC3 — Find and highlight in textarea")));
        TextArea content = find(TextArea.class).single();
        assertTrue(content.getValue().contains("Lorem ipsum"));
    }

    @Test
    void findNextReportsTheNextMatchOffsetAndWraps() {
        navigate(FindAndHighlightView.class);
        runPendingSignalsTasks();

        TextField search = find(TextField.class).single();
        TextArea content = find(TextArea.class).single();
        Button findNext = find(Button.class).withText("Find next").single();

        test(search).setValue("dolor");

        test(findNext).click();
        runPendingSignalsTasks();
        int firstOffset = readMatchOffset();
        assertEquals("dolor",
                content.getValue().substring(firstOffset, firstOffset + 5));

        test(findNext).click();
        runPendingSignalsTasks();
        int secondOffset = readMatchOffset();
        assertTrue(secondOffset > firstOffset,
                "second match should be past the first");
        assertEquals("dolor",
                content.getValue().substring(secondOffset, secondOffset + 5));
    }

    private int readMatchOffset() {
        String status = findInView(Span.class).all().stream().map(Span::getText)
                .filter(t -> t != null && t.contains("Match at ")).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected a 'Match at N' status span"));
        // status is "Match at <N>" or "Wrapped to start — match at <N>"
        int idx = status.lastIndexOf(' ');
        return Integer.parseInt(status.substring(idx + 1));
    }

    @Test
    void emptySearchTermShowsHint() {
        navigate(FindAndHighlightView.class);
        // Simulate the user clicking around in the textarea so a non-empty
        // selection signal value exists from a prior interaction.
        TextArea content = find(TextArea.class).single();
        TextSelectionTestSupport.setSelection(content, 0, 0);
        runPendingSignalsTasks();

        Button findNext = find(Button.class).withText("Find next").single();
        test(findNext).click();
        runPendingSignalsTasks();

        assertTrue(findInView(Span.class).all().stream().anyMatch(
                s -> "Enter a search term first".equals(s.getText())));
    }
}
