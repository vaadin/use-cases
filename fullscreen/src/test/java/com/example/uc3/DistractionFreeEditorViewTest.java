package com.example.uc3;

import com.example.FullscreenTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DistractionFreeEditorView.class)
class DistractionFreeEditorViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithEditorAndExpandButton() {
        navigate(DistractionFreeEditorView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> h.getText().equals("UC3 — Distraction-free editor")));
        assertTrue(!findInView(TextArea.class).all().isEmpty());
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Expand to fullscreen".equals(b.getText())));
        // Done is bindVisible(fullscreen), so it is hidden (and not surfaced
        // by findInView) while the page is not fullscreen.
        assertFalse(findInView(Button.class).all().stream()
                .anyMatch(b -> "Done".equals(b.getText())));
    }

    @Test
    void doneButtonAppearsOnlyWhileFullscreen() {
        navigate(DistractionFreeEditorView.class);
        runPendingSignalsTasks();

        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertTrue(
                findInView(Button.class).all().stream()
                        .anyMatch(b -> "Done".equals(b.getText())),
                "Done button should be visible while fullscreen");

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertFalse(
                findInView(Button.class).all().stream()
                        .anyMatch(b -> "Done".equals(b.getText())),
                "Done button should hide again when leaving fullscreen");
    }

    @Test
    void wordCountUpdatesOnTextChange() {
        navigate(DistractionFreeEditorView.class);
        runPendingSignalsTasks();

        TextArea editor = findInView(TextArea.class).single();
        test(editor).setValue("hello world this is fullscreen");
        runPendingSignalsTasks();

        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> "5 words".equals(s.getText())),
                "word count should report 5 words after typing");
    }

    @Test
    void stateBadgeReflectsFullscreenSignal() {
        navigate(DistractionFreeEditorView.class);
        runPendingSignalsTasks();

        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        // Badge is hidden by the fullscreen wrapper, so it intentionally
        // stays on the idle copy rather than flipping to a hidden message.
        assertBadgeContains("Click Expand");

        FullscreenTestSupport.setFullscreenState(FullscreenState.UNSUPPORTED);
        runPendingSignalsTasks();
        assertBadgeContains("not supported");
    }

    private void assertBadgeContains(String fragment) {
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getClassNames().contains("status-badge")
                        && s.getText() != null
                        && s.getText().contains(fragment)),
                "expected status badge to contain \"" + fragment + "\"");
    }
}
