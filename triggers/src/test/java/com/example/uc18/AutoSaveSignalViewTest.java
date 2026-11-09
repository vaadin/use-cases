package com.example.uc18;

import com.example.TriggerCallbacks;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = AutoSaveSignalView.class)
class AutoSaveSignalViewTest extends SpringBrowserlessTest {

    @Test
    void countsFollowTheTextEachKeystrokeSends() {
        navigate(AutoSaveSignalView.class);
        assertEquals("UC18 — Auto-save signal",
                findInView(H1.class).single().getText());
        Span chars = findInView(Span.class).id("chars");
        Span words = findInView(Span.class).id("words");
        assertEquals("0 characters", chars.getText());
        assertEquals("0 words", words.getText());

        TextArea draft = findInView(TextArea.class).id("draft");
        TriggerCallbacks.fire(draft, "Hello");
        runPendingSignalsTasks();
        assertEquals("5 characters", chars.getText());
        assertEquals("1 word", words.getText());

        TriggerCallbacks.fire(draft, "Hello trigger world");
        runPendingSignalsTasks();
        assertEquals("19 characters", chars.getText());
        assertEquals("3 words", words.getText());
    }
}
