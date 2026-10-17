package com.example.uc6;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;

import static com.example.TextSelectionTestSupport.selectInBrowser;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = LiveSelectionInfoView.class)
class LiveSelectionInfoViewTest extends SpringBrowserlessTest {

    @Test
    void panelFollowsTheSelection() {
        navigate(LiveSelectionInfoView.class);
        runPendingSignalsTasks();
        assertEquals(List.of("—", "0 chars", "0 words"), values());
        assertEquals("(no selection)", preview());

        TextArea text = find(TextArea.class).single();
        // "The quick brown fox"
        selectInBrowser(text, 0, 19);
        runPendingSignalsTasks();
        assertEquals(List.of("0 – 19", "19 chars", "4 words"), values());
        assertEquals("The quick brown fox", preview());

        // Clicking places the cursor and clears the selection.
        selectInBrowser(text, 5, 5);
        runPendingSignalsTasks();
        assertEquals(List.of("—", "0 chars", "0 words"), values());
        assertEquals("(no selection)", preview());
    }

    private List<String> values() {
        return find(Span.class).withClassName("uc6-value").all().stream()
                .map(Span::getText).toList();
    }

    private String preview() {
        return find(Div.class).withClassName("uc6-preview").single().getText();
    }
}
