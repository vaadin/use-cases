package com.example.uc6;

import com.example.TextSelectionTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LiveSelectionInfoView.class)
class LiveSelectionInfoViewTest extends SpringBrowserlessTest {

    @Test
    void viewRenders() {
        navigate(LiveSelectionInfoView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC6 — Live selection info".equals(h.getText())));
    }

    @Test
    void selectingTextUpdatesBoundLabelsReactively() {
        navigate(LiveSelectionInfoView.class);
        runPendingSignalsTasks();

        // Initially nothing selected
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "0 chars".equals(s.getText())));
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "0 words".equals(s.getText())));

        TextArea text = find(TextArea.class).single();
        // "The quick brown fox" — 19 chars, 4 words
        TextSelectionTestSupport.setSelection(text, 0, 19);
        runPendingSignalsTasks();

        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> "19 chars".equals(s.getText())),
                "expected selection length to update");
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> "4 words".equals(s.getText())),
                "expected word count to update");
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> "0 – 19".equals(s.getText())),
                "expected range to update");
    }
}
